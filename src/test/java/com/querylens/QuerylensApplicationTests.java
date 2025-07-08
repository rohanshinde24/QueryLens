// package com.querylens;

// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;

// @SpringBootTest
// class QuerylensApplicationTests {

// 	@Test
// 	void contextLoads() {
// 	}

// }
package com.querylens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    // disable Hibernate DDL auto for safety
    "spring.jpa.hibernate.ddl-auto=none"
  }
)
class QuerylensApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the full Spring context (including your rewriters & JdbcTemplate)
        // starts up against an in-memory H2 database.
    }
}
