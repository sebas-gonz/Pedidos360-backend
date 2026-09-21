package com.seb.pedidos360.order;

import org.springframework.boot.SpringApplication;

public class TestMsPedidos360OrderApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsPedidos360OrderApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
