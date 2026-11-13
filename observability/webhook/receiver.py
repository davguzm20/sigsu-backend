"""Receptor local de demostración; no notifica a personas ni registra cabeceras."""
import json
import os
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

LOCK = threading.Lock()
PATH = "/data/notifications.jsonl"
LABELS = {"alertname", "grafana_folder", "service", "scope", "severity", "rule_uid"}


class Receiver(BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.send_response(200 if self.path == "/health" else 404)
        self.end_headers()

    def do_POST(self):
        if self.path != "/alerts":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", "0"))
        if not 0 < length <= 1024 * 1024:
            self.send_error(413)
            return
        try:
            payload = json.loads(self.rfile.read(length))
            record = {
                "receivedAtUtc": datetime.now(timezone.utc).isoformat(),
                "receiver": str(payload.get("receiver", "")),
                "status": str(payload["status"]),
                "alerts": [
                    {
                        "status": alert.get("status"),
                        "startsAt": alert.get("startsAt"),
                        "endsAt": alert.get("endsAt"),
                        "labels": {k: v for k, v in alert.get("labels", {}).items() if k in LABELS},
                        "summary": alert.get("annotations", {}).get("summary", ""),
                        "description": alert.get("annotations", {}).get("description", ""),
                    }
                    for alert in payload.get("alerts", [])
                ],
            }
            if record["status"] not in {"firing", "resolved"}:
                raise ValueError("Unsupported status")
        except (ValueError, KeyError, TypeError):
            self.send_error(400)
            return
        with LOCK:
            with open(PATH, "a", encoding="utf-8") as output:
                output.write(json.dumps(record, ensure_ascii=False) + "\n")
                output.flush()
                os.fsync(output.fileno())
        print(json.dumps({"event": "local.notification.received", "status": record["status"],
                          "receivedAtUtc": record["receivedAtUtc"], "alerts": len(record["alerts"])}), flush=True)
        self.send_response(200)
        self.end_headers()


ThreadingHTTPServer(("0.0.0.0", 8080), Receiver).serve_forever()
