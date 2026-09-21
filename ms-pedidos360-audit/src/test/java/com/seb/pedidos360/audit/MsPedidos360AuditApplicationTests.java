package com.seb.pedidos360.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MsPedidos360AuditApplicationTests {

	@Test
	void contextLoads() {
	}

}
