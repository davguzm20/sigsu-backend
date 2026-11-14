package com.sigsu.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import com.sigsu.backend.common.configs.CloudinaryProperties;
import com.sigsu.backend.common.configs.CorsProperties;
import com.sigsu.backend.common.security.JwtProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, CloudinaryProperties.class})
public class SigsuBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigsuBackendApplication.class, args);
	}

}
