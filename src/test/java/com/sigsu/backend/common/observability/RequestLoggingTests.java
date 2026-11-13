package com.sigsu.backend.common.observability;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.common.security.JwtService;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "observability"})
@AutoConfigureTracing
@Import(RequestLoggingTests.TestComponents.class)
@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingTests {
    @LocalServerPort int port;
    @Autowired JwtService jwtService;
    @Autowired RecordingProcessor spans;
    @Autowired RequestLoggingFilter filter;
    @MockitoBean UsuarioRepository usuarios;

    @Test
    void realHttpRequestsHaveOneSummaryAndOneRealServerSpan(CapturedOutput output) throws Exception {
        UsuarioEntity user = new UsuarioEntity();
        user.setCorreo("logging-test@example.invalid");
        user.setRol(RolEnum.SOLICITANTE);
        when(usuarios.findByCorreo(user.getCorreo())).thenReturn(Optional.of(user));
        String jwt = jwtService.generateToken(user);
        String canary = "private-canary-" + UUID.randomUUID();
        HttpClient client = HttpClient.newHttpClient();
        var requests = List.of(
                HttpRequest.newBuilder(uri("/actuator/health")).header("X-Trace-Id", canary).GET().build(),
                HttpRequest.newBuilder(uri("/api/__test/items/" + canary + "?password=" + canary))
                        .header("Authorization", "Bearer " + jwt).header("Cookie", "secret=" + canary).GET().build(),
                HttpRequest.newBuilder(uri("/api/auth/login" )).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"contrasena\":\"" + canary + "\"}")).build(),
                HttpRequest.newBuilder(uri("/api/usuarios/" + canary)).header("Authorization", "Bearer " + canary).GET().build(),
                HttpRequest.newBuilder(uri("/actuator/prometheus")).GET().build(),
                HttpRequest.newBuilder(uri("/actuator/shutdown")).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpRequest.newBuilder(uri("/api/usuarios")).header("Authorization", "Bearer " + jwt).GET().build(),
                HttpRequest.newBuilder(uri("/api/__test/fail")).header("Authorization", "Bearer " + jwt).GET().build());
        int[] statuses = {200, 200, 400, 401, 401, 403, 403, 500};
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            var response = client.send(requests.get(i), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(statuses[i]);
            String id = response.headers().firstValue("X-Trace-Id").orElseThrow();
            assertThat(id).matches("[0-9a-f]{32}");
            ids.add(id);
        }
        // El span termina inmediatamente después de completar el filtro; esperar solo ese cierre.
        for (int retry = 0; retry < 100 && spans.finished.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER).count() < requests.size(); retry++) Thread.sleep(20);
        var json = new ObjectMapper();
        assertThat(ids).doesNotHaveDuplicates();
        String logs = output.getOut();
        assertThat(logs).doesNotContain(canary, jwt, user.getCorreo());
        var events = logs.lines().filter(line -> line.startsWith("{"))
                .map(json::readTree).toList();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            var summaries = events.stream().filter(e -> "http.request".equals(e.path("event").asText())
                    && id.equals(e.path("traceId").asText())).toList();
            assertThat(summaries).hasSize(1);
            var event = summaries.getFirst();
            assertThat(event.path("status").asInt()).isEqualTo(statuses[i]);
            assertThat(event.path("durationMs").asDouble()).isGreaterThanOrEqualTo(0);
            assertThat(event.path("@timestamp").asText()).isNotBlank();
            assertThat(event.path("level").asText()).isEqualTo("INFO");
            assertThat(event.path("service").asText()).isEqualTo("sigsu-backend");
            assertThat(event.path("message").asText()).isEqualTo("HTTP request completed");
            var recorded = spans.finished.stream().filter(s -> id.equals(s.getTraceId())
                    && s.getKind() == SpanKind.SERVER).toList();
            assertThat(recorded).hasSize(1);
            assertThat(recorded.getFirst().getSpanId()).isEqualTo(event.path("spanId").asText());
            assertThat(recorded.getFirst().getSpanContext().isSampled()).isTrue();
            if (statuses[i] == 500) {
                assertThat(recorded.getFirst().getAttributes().get(AttributeKey.stringKey("exception")))
                        .isEqualTo("IllegalStateException");
                var errors = events.stream().filter(e -> id.equals(e.path("traceId").asText())
                        && "ERROR".equals(e.path("level").asText())).toList();
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst().path("stack_trace").asText()).contains("Controlled test failure");
            }
        }
        assertThat(events.stream().filter(e -> "http.request".equals(e.path("event").asText()))
                .anyMatch(e -> "/api/__test/items/{id}".equals(e.path("path").asText()))).isTrue();
        assertThat(org.slf4j.MDC.get("traceId")).isNull();
        // Un despacho ERROR no debe producir un segundo resumen ni otro identificador.
        int before = output.getOut().length();
        var errorRequest = new org.springframework.mock.web.MockHttpServletRequest();
        errorRequest.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
        errorRequest.setAttribute(org.springframework.web.util.WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/api/__test/fail");
        var errorResponse = new org.springframework.mock.web.MockHttpServletResponse();
        boolean[] continued = {false};
        filter.doFilter(errorRequest, errorResponse, (request, response) -> continued[0] = true);
        assertThat(continued[0]).isTrue();
        assertThat(errorResponse.getHeader("X-Trace-Id")).isNull();
        assertThat(output.getOut().substring(before)).doesNotContain("HTTP request completed");
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestComponents {
        @Bean RecordingProcessor recordingProcessor() { return new RecordingProcessor(); }
        @Bean TestController testController() { return new TestController(); }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/__test/items/{id}") String success() { return "ok"; }
        @GetMapping("/api/__test/fail") String fail() { throw new IllegalStateException("Controlled test failure"); }
    }

    static class RecordingProcessor implements SpanProcessor {
        final List<SpanData> finished = new CopyOnWriteArrayList<>();
        public void onStart(Context parent, ReadWriteSpan span) { }
        public boolean isStartRequired() { return false; }
        public void onEnd(ReadableSpan span) { finished.add(span.toSpanData()); }
        public boolean isEndRequired() { return true; }
    }
}
