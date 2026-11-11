package com.rdn.rdn_api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Executa o DDL real do Flyway sobre um banco vazio.
 *
 * <p>Objetivo: garantir que {@code V1__schema.sql} e {@code V2__seed.sql} sao
 * aplicaveis do zero e que o seed produz os registros esperados, sem depender de
 * um PostgreSQL rodando na maquina de desenvolvimento.</p>
 */
class SchemaDdlTest {

    @Test
    void aplicaSchemaESeedEmBancoLimpo() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:schema_" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/migration/V1__schema.sql"));
        populator.addScript(new ClassPathResource("db/migration/V2__seed.sql"));
        populator.execute(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "branches")).isEqualTo(1);
            assertThat(count(statement, "menus")).isEqualTo(1);
            assertThat(count(statement, "menu_items")).isEqualTo(3);
            assertThat(count(statement, "inventory_items")).isEqualTo(3);
            assertThat(count(statement, "inventory_movements")).isEqualTo(3);
        }
    }

    private static long count(Statement statement, String table) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

}
