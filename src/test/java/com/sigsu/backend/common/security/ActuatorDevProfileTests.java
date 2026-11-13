package com.sigsu.backend.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class ActuatorDevProfileTests {
    @Autowired WebEndpointsSupplier webEndpoints;

    @Test
    void onlyHealthIsExposedWithoutObservabilityProfile() {
        assertThat(webEndpoints.getEndpoints()).extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactly("health");
    }
}
