FROM python:3.14.2-alpine3.23
RUN addgroup -g 10001 receiver && adduser -D -u 10001 -G receiver receiver && mkdir /data && chown receiver:receiver /data
WORKDIR /app
COPY receiver.py /app/receiver.py
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
USER 10001:10001
CMD ["python", "/app/receiver.py"]
