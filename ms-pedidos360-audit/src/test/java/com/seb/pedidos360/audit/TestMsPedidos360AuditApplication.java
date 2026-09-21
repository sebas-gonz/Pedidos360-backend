package com.seb.pedidos360.audit;

import org.springframework.boot.SpringApplication;

public class TestMsPedidos360AuditApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsPedidos360AuditApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
