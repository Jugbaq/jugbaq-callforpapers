package com.jugbaq.cfp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flyway_migration_creates_all_expected_tables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                Integer.class
        );
        // 16 domain tables + event_publication + flyway_schema_history = 18
        assertThat(tableCount).isEqualTo(18);
    }

    @Test
    void default_jugbaq_tenant_is_seeded() {
        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM tenants WHERE slug = 'jugbaq'",
                String.class
        );
        assertThat(name).isEqualTo("Java User Group Barranquilla");
    }
}
