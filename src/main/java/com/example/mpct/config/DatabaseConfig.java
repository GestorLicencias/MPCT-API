package com.example.mpct.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixEnumConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE tramites DROP CONSTRAINT IF EXISTS tramites_tipo_check");
            jdbcTemplate.execute("ALTER TABLE tramites DROP CONSTRAINT IF EXISTS tramites_estado_check");
            System.out.println("Constraints de enums eliminados correctamente.");
        } catch (Exception e) {
            System.out.println("No se pudieron eliminar los constraints: " + e.getMessage());
        }
    }
}
