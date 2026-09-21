package com.seb.pedidos360.notify;

import org.springframework.boot.SpringApplication;

public class TestMsPedidos360NotifyApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsPedidos360NotifyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
