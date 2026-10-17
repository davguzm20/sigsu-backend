package com.sigsu.backend.common.configs;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminUserInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                               Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = environment.getProperty("SIGSU_ADMIN_CORREO");
        String password = environment.getProperty("SIGSU_ADMIN_CONTRASENA");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }

        if (usuarioRepository.existsByCorreo(email)) {
            return;
        }

        UsuarioEntity user = new UsuarioEntity();
        user.setNombres("Administrador");
        user.setApellidos("SIGSU");
        user.setCorreo(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRol(RolEnum.ADMINISTRADOR);
        user.setActivo(true);

        usuarioRepository.save(user);

        log.info("Usuario administrador creado: {}", email);
    }
}
