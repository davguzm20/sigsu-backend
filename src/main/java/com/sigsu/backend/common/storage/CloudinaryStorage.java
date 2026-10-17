package com.sigsu.backend.common.storage;

import com.sigsu.backend.common.configs.CloudinaryProperties;
import com.sigsu.backend.common.exceptions.StorageException;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class CloudinaryStorage {

    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/%s/auto/upload";
    private static final String UPLOAD_ERROR = "No se pudo guardar el archivo. Intente de nuevo en unos minutos";

    private final CloudinaryProperties properties;
    private final RestClient restClient;

    public CloudinaryStorage(CloudinaryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String upload(MultipartFile file, String publicId) {
        Map<String, String> params = new TreeMap<>();
        params.put("folder", properties.folder());
        params.put("public_id", publicId);
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", file.getResource()).filename(file.getOriginalFilename());
        builder.part("api_key", properties.apiKey());
        params.forEach(builder::part);
        builder.part("signature", signature(params));

        try {
            Map<?, ?> response = restClient.post()
                    .uri(UPLOAD_URL.formatted(properties.cloudName()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);

            Object secureUrl = response == null ? null : response.get("secure_url");
            if (secureUrl == null) {
                throw new StorageException(UPLOAD_ERROR);
            }

            return secureUrl.toString();
        } catch (RestClientException ex) {
            throw new StorageException(UPLOAD_ERROR, ex);
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
