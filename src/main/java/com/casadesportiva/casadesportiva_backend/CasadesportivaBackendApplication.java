package com.casadesportiva.casadesportiva_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.casadesportiva")
@EntityScan(basePackages = "com.casadesportiva.model")
@EnableJpaRepositories(basePackages = "com.casadesportiva.repository")
public class CasadesportivaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CasadesportivaBackendApplication.class, args);
    }
}