package com.sigsu.backend.common.storage;

import com.sigsu.backend.common.configs.CloudinaryProperties;
import com.sigsu.backend.common.exceptions.StorageException;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class CloudinaryStorage {

    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/{cloud}/auto/upload";
    private static final String UPLOAD_ERROR = "No se pudo guardar el archivo. Intente de nuevo en unos minutos";

    private final CloudinaryProperties properties;
    private final RestClient restClient;
    private final ObservationRegistry observationRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    public CloudinaryStorage(CloudinaryProperties properties, RestClient.Builder builder,
                             ObservationRegistry observationRegistry) {
        this.properties = properties;
        this.observationRegistry = observationRegistry;
        this.restClient = builder.clone()
                .observationConvention(new DefaultClientRequestObservationConvention() {
                    @Override
                    public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
                        // No exportar la URL: contiene el nombre de la cuenta Cloudinary.
                        return KeyValues.empty();
                    }
                    @Override
                    public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
                        return super.getLowCardinalityKeyValues(context)
                                .and("uri", "/v1_1/{cloud}/auto/upload").and("client.name", "cloudinary");
                    }
                })
                .requestInterceptor((request, body, execution) -> {
                    try {
                        return execution.execute(request, body);
                    } catch (IOException ex) {
                        // Antes de que RestClient incluya la URL en una ResourceAccessException.
                        throw new UploadFailure("connection_error");
                    }
                }).build();
    }

    public String upload(MultipartFile file, String publicId) {
        Observation observation = Observation.createNotStarted("storage.upload", observationRegistry)
                .lowCardinalityKeyValue("provider", "cloudinary")
                .lowCardinalityKeyValue("operation", "upload").start();
        String result = "success";
        try (Observation.Scope scope = observation.openScope()) {
            return performUpload(file, publicId);
        } catch (UploadFailure ex) {
            result = ex.result;
            observation.error(ex);
            throw new StorageException(UPLOAD_ERROR, ex);
        } catch (RuntimeException ex) {
            result = "unexpected_error";
            // No conservar mensajes o causas que puedan contener credenciales o payloads.
            UploadFailure safe = new UploadFailure(result);
            observation.error(safe);
            throw new StorageException(UPLOAD_ERROR, safe);
        } finally {
            observation.lowCardinalityKeyValue("result", result);
            observation.stop();
        }
    }

    private String performUpload(MultipartFile file, String publicId) {
        Map<String, String> params = new TreeMap<>();
        params.put("folder", properties.folder());
        params.put("public_id", publicId);
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource()).filename(file.getOriginalFilename());
        builder.part("api_key", properties.apiKey());
        params.forEach(builder::part);
        builder.part("signature", signature(params));

        return restClient.post()
                    .uri(UPLOAD_URL, properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) throw new UploadFailure("http_error");
                        try {
                            Map<?, ?> payload = mapper.readValue(response.getBody(), Map.class);
                            Object url = payload == null ? null : payload.get("secure_url");
                            if (!(url instanceof String value) || value.isBlank()) {
                                throw new UploadFailure("invalid_response");
                            }
                            return value;
                        } catch (UploadFailure ex) {
                            throw ex;
                        } catch (RuntimeException | IOException ex) {
                            throw new UploadFailure("invalid_response");
                        }
                    });
    }

    private static final class UploadFailure extends RestClientException {
        private final String result;
        private UploadFailure(String result) {
            super("Cloudinary upload failed: " + result);
            this.result = result;
        }
    }

    private String signature(Map<String, String> params) {
        String joined = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        try {
            byte[] hash = MessageDigest.getInstance("SHA-1")
                    .digest((joined + properties.apiSecret()).getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new StorageException(UPLOAD_ERROR, ex);
        }
    }
}
