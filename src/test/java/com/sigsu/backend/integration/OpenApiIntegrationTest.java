package com.sigsu.backend.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("API-20 la documentación OpenAPI lista las 36 operaciones con descripción")
    void exposesOpenApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> operations = JsonPath.read(result.getResponse().getContentAsString(), "$.paths.*.*");

        assertThat(operations).hasSize(36);
        assertThat(operations).allSatisfy(operation -> assertThat(operation.get("summary")).isNotNull());
    }
}
