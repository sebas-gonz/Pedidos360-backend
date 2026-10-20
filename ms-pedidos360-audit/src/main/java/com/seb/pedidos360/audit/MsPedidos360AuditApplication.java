package com.seb.pedidos360.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication
@EnableR2dbcAuditing
public class MsPedidos360AuditApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsPedidos360AuditApplication.class, args);
	}

}
