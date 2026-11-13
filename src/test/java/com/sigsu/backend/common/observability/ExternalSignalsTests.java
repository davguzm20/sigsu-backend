package com.sigsu.backend.common.observability;

import com.sigsu.backend.auth.entities.*;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.common.configs.CloudinaryProperties;
import com.sigsu.backend.common.security.*;
import com.sigsu.backend.common.storage.CloudinaryStorage;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.system.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "observability"})
@AutoConfigureTracing
@AutoConfigureMetrics
@Import(ExternalSignalsTests.Components.class)
@ExtendWith(OutputCaptureExtension.class)
class ExternalSignalsTests {
    @LocalServerPort int port;
    @Autowired RestClient.Builder builder;
    @Autowired ObservationRegistry observations;
    @Autowired MeterRegistry meters;
    @Autowired JwtService jwt;
    @Autowired JwtProperties jwtProperties;
    @Autowired TestController controller;
    @Autowired RequestLoggingTests.RecordingProcessor spans;
    @MockitoBean UsuarioRepository usuarios;
    final HttpClient http = HttpClient.newHttpClient();

    @Test
    void storageUsesOneOperationAndOneHttpSpanWithBoundedMetrics(CapturedOutput output) throws Exception {
        String privateValue = "private-storage-" + UUID.randomUUID();
        UsuarioEntity user = user();
        when(usuarios.findByCorreo(user.getCorreo())).thenReturn(Optional.of(user));
        String token = jwt.generateToken(user);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<Integer> status = new AtomicReference<>(200);
        AtomicReference<String> traceparent = new AtomicReference<>();
        AtomicReference<Set<String>> headerNames = new AtomicReference<>();
        HttpServer mock = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mock.createContext("/upload", exchange -> {
            traceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
            headerNames.set(new HashSet<>(exchange.getRequestHeaders().keySet()));
            exchange.getRequestBody().transferTo(OutputStream.nullOutputStream());
            byte[] bytes = body.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        mock.start();
        try {
            String[][] scenarios = {
                    {"success", "200", "{\"secure_url\":\"https://example.invalid/" + privateValue + "\"}"},
                    {"http_error", "401", "{\"error\":\"" + privateValue + "\"}"},
                    {"http_error", "503", "{\"error\":\"" + privateValue + "\"}"},
                    {"success", "200", "{\"secure_url\":\"https://example.invalid/" + privateValue + "\"}", "recovery"},
                    {"invalid_response", "200", "{\"private\":\"" + privateValue + "\"}"},
                    {"invalid_response", "200", "{\"secure_url\":null}"},
                    {"invalid_response", "200", "{\"secure_url\":42}"},
                    {"invalid_response", "200", "{\"secure_url\":\" \"}"},
                    {"invalid_response", "200", "malformed-" + privateValue},
                    {"connection_error", "0", ""}
            };
            Map<String, Integer> counts = new HashMap<>();
            for (String[] scenario : scenarios) {
                int target = mock.getAddress().getPort();
                if (scenario[0].equals("connection_error")) {
                    try (ServerSocket unavailable = new ServerSocket(0)) { target = unavailable.getLocalPort(); }
                }
                int destination = target;
                body.set(scenario[2]);
                status.set(Integer.parseInt(scenario[1]));
                // Redirigir solamente el transporte en pruebas: la URL de producción no se vuelve configurable.
                var factory = new SimpleClientHttpRequestFactory() {
                    @Override public ClientHttpRequest createRequest(URI ignored, HttpMethod method) throws IOException {
                        return super.createRequest(URI.create("http://127.0.0.1:" + destination + "/upload"), method);
                    }
                };
                controller.storage = new CloudinaryStorage(new CloudinaryProperties(privateValue,
                        privateValue, privateValue, privateValue), builder.clone().requestFactory(factory), observations);
                controller.privateValue = privateValue;
                var response = request("/api/__test/storage", token);
                assertThat(response.statusCode()).isEqualTo(scenario[0].equals("success") ? 200 : 502);
                String id = response.headers().firstValue("X-Trace-Id").orElseThrow();
                List<SpanData> trace = finishedTrace(id);
                var server = trace.stream().filter(s -> s.getKind() == SpanKind.SERVER).toList();
                var storage = trace.stream().filter(s -> s.getName().equals("storage.upload")).toList();
                var client = trace.stream().filter(s -> s.getKind() == SpanKind.CLIENT).toList();
                assertThat(server).hasSize(1);
                assertThat(storage).hasSize(1);
                assertThat(client).hasSize(1);
                assertThat(client.getFirst().getParentSpanId()).isEqualTo(storage.getFirst().getSpanId());
                String ancestor = storage.getFirst().getParentSpanId();
                Set<String> seen = new HashSet<>();
                while (!ancestor.equals(server.getFirst().getSpanId())) {
                    assertThat(seen.add(ancestor)).isTrue();
                    String parent = ancestor;
                    ancestor = trace.stream().filter(s -> s.getSpanId().equals(parent)).findFirst().orElseThrow().getParentSpanId();
                }
                assertThat(trace.toString()).doesNotContain(privateValue, token);
                if (!scenario[0].equals("connection_error")) {
                    assertThat(traceparent.get()).as("Nombres de cabeceras recibidas: %s", headerNames.get())
                            .contains(id, client.getFirst().getSpanId());
                }
                counts.merge(scenario[0], 1, Integer::sum);
                var timer = meters.find("storage.upload").tags("provider", "cloudinary", "operation", "upload",
                        "result", scenario[0]).timer();
                assertThat(timer).isNotNull();
                assertThat(timer.count()).isEqualTo(counts.get(scenario[0]).longValue());
                assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);
                if (scenario[1].equals("503") || scenario.length > 3) {
                    boolean recovery = scenario.length > 3;
                    assertThat(client.getFirst().getAttributes()
                            .get(io.opentelemetry.api.common.AttributeKey.stringKey("status")))
                            .isEqualTo(recovery ? "200" : "503");
                    assertIncidentLogs(id, response.statusCode(), output);
                }
            }
            assertThat(meters.getMeters().stream().map(m -> m.getId().toString()).toList().toString())
                    .doesNotContain(privateValue, token);
            assertThat(output.getOut()).doesNotContain(privateValue, token, user.getCorreo());
            var json = new ObjectMapper();
            var logs = output.getOut().lines().filter(l -> l.startsWith("{")).map(json::readTree).toList();
            assertThat(logs.stream().filter(l -> "ERROR".equals(l.path("level").asText())).count()).isEqualTo(8);
        } finally { mock.stop(0); }
    }

    private void assertIncidentLogs(String id, int responseStatus, CapturedOutput output) {
        var json = new ObjectMapper();
        var logs = output.getOut().lines().filter(l -> l.startsWith("{")).map(json::readTree)
                .filter(l -> id.equals(l.path("traceId").asText())).toList();
        var summaries = logs.stream().filter(l -> "http.request".equals(l.path("event").asText())).toList();
        var errors = logs.stream().filter(l -> "ERROR".equals(l.path("level").asText())).toList();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().path("status").asInt()).isEqualTo(responseStatus);
        assertThat(summaries.getFirst().path("durationMs").asDouble()).isGreaterThan(0);
        assertThat(errors).hasSize(responseStatus == 502 ? 1 : 0);
        if (!errors.isEmpty()) {
            assertThat(errors.getFirst().path("stack_trace").asText()).contains("StorageException", "http_error");
        }
    }

    @Test
    void technicalJwtFailureIsVisibleButInvalidTokensRemainRejected(CapturedOutput output) throws Exception {
        UsuarioEntity user = user();
        String token = jwt.generateToken(user);
        String secretMessage = "private-db-message-" + UUID.randomUUID();
        when(usuarios.findByCorreo(user.getCorreo())).thenThrow(new DataAccessResourceFailureException(secretMessage));
        var failed = request("/api/usuarios/roles", token);
        assertThat(failed.statusCode()).isEqualTo(401);
        String id = failed.headers().firstValue("X-Trace-Id").orElseThrow();
        finishedTrace(id);
        var expired = new JwtService(new JwtProperties(jwtProperties.secret(), -60_000)).generateToken(user);
        assertThat(request("/api/usuarios/roles", expired).statusCode()).isEqualTo(401);
        assertThat(request("/api/usuarios/roles", "invalid-token").statusCode()).isEqualTo(401);
        verify(usuarios, times(1)).findByCorreo(user.getCorreo());
        var json = new ObjectMapper();
        var errors = output.getOut().lines().filter(l -> l.startsWith("{")).map(json::readTree)
                .filter(l -> "auth.jwt.technical_failure".equals(l.path("event").asText())).toList();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().path("traceId").asText()).isEqualTo(id);
        assertThat(errors.getFirst().path("spanId").asText()).matches("[0-9a-f]{16}");
        assertThat(errors.getFirst().path("stage").asText()).isEqualTo("user_lookup");
        assertThat(errors.getFirst().path("stack_trace").asText()).contains("JWT technical failure");
        assertThat(output.getOut()).doesNotContain(secretMessage, token, expired, user.getCorreo());
    }

    private UsuarioEntity user() {
        var user = new UsuarioEntity();
        user.setCorreo("external-signals@example.invalid");
        user.setRol(RolEnum.SOLICITANTE);
        return user;
    }

    private HttpResponse<String> request(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private List<SpanData> finishedTrace(String id) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            var trace = spans.finished.stream().filter(s -> s.getTraceId().equals(id)).toList();
            if (trace.stream().anyMatch(s -> s.getKind() == SpanKind.SERVER)) return trace;
            Thread.sleep(20);
        }
        throw new AssertionError("No terminó el span servidor");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Components {
        @Bean RequestLoggingTests.RecordingProcessor recordingProcessor() { return new RequestLoggingTests.RecordingProcessor(); }
        @Bean TestController storageTestController() { return new TestController(); }
    }
    @RestController
    static class TestController {
        volatile CloudinaryStorage storage;
        volatile String privateValue;
        @GetMapping("/api/__test/storage") String upload() {
            return storage.upload(new MockMultipartFile("file", privateValue + ".png", "image/png",
                    privateValue.getBytes(java.nio.charset.StandardCharsets.UTF_8)), privateValue);
        }
    }
}
