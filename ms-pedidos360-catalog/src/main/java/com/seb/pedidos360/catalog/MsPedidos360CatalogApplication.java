package com.seb.pedidos360.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication
@EnableR2dbcAuditing
public class MsPedidos360CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPedidos360CatalogApplication.class, args);
    }

}
