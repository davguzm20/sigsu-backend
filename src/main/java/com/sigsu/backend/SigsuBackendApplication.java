package com.sigsu.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.sigsu.backend.common.security.JwtProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
@EnableConfigurationProperties(JwtProperties.class)
public class SigsuBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigsuBackendApplication.class, args);
	}

}
