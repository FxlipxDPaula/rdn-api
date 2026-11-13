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
 * <p>Objetivo: garantir que {@code V1__schema.sql}, {@code V2__seed.sql},
 * {@code V3__seed_users.sql} e {@code V4__add_awaiting_payment_status.sql} sao
 * aplicaveis do zero, que os seeds produzem os registros esperados e que a
 * constraint de status aceita o novo valor {@code AGUARDANDO_PAGAMENTO}, sem
 * depender de um PostgreSQL rodando na maquina de desenvolvimento.</p>
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
        populator.addScript(new ClassPathResource("db/migration/V3__seed_users.sql"));
        populator.addScript(new ClassPathResource("db/migration/V4__add_awaiting_payment_status.sql"));
        populator.execute(dataSource);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            assertThat(count(statement, "branches")).isEqualTo(1);
            assertThat(count(statement, "menus")).isEqualTo(1);
            assertThat(count(statement, "menu_items")).isEqualTo(3);
            assertThat(count(statement, "inventory_items")).isEqualTo(3);
            assertThat(count(statement, "inventory_movements")).isEqualTo(3);

            assertThat(count(statement, "users")).isEqualTo(4);
            assertThat(countUsersWithBcryptHash(statement)).isEqualTo(4);

            // V4 ampliou a constraint de status. A verificacao e feita pelo
            // catalogo do banco, e nao por INSERT: no H2 2.x a expressao
            // CHECK criada na sessao do populator guarda um cache ligado a
            // aquela sessao e falha ao ser avaliada depois que ela fecha.
            assertThat(checkClause(statement, "ck_orders_status"))
                    .contains("AGUARDANDO_PAGAMENTO")
                    .contains("RECEIVED")
                    .doesNotContain("PENDING");
        }
    }

    private static String checkClause(Statement statement, String constraintName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                        + "WHERE CONSTRAINT_NAME = '" + constraintName + "'")) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private static long count(Statement statement, String table) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static long countUsersWithBcryptHash(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM users WHERE password_hash LIKE '$2a$%'")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

}
