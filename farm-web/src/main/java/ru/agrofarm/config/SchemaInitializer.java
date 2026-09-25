package ru.agrofarm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Order(0)
public class SchemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);
    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres")) return;
            try (ResultSet rs = c.getMetaData().getTables(null, "public", "users", new String[]{"TABLE"})) {
                if (rs.next()) return;
            }
            log.info("Таблицы не найдены — создаю схему из db/agrofarm.sql");
            String sql = new String(new ClassPathResource("db/agrofarm.sql").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
        }
    }
}
