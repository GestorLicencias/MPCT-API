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
            System.out.println("Constraint tramites_tipo_check eliminado correctamente.");
        } catch (Exception e) {
            System.out.println("No se pudo eliminar el constraint tramites_tipo_check: " + e.getMessage());
        }
    }
}
