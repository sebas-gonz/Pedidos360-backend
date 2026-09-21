package com.seb.pedidos360.catalog;

import org.springframework.boot.SpringApplication;

public class TestMsPedidos360CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsPedidos360CatalogApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
