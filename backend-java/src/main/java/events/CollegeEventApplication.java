package events;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class CollegeEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollegeEventApplication.class, args);
    }

    @Bean
    public DataSource dataSource(
            @Value("${app.database.path:../database/database.sqlite}") String rawPath,
            @Value("${spring.datasource.hikari.maximum-pool-size:1}") int maximumPoolSize) throws IOException {
        Path dbPath = resolveDatabasePath(rawPath);
        if (dbPath.getParent() != null) {
            Files.createDirectories(dbPath.getParent());
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbPath.toAbsolutePath().normalize());
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionInitSql("PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000;");
        return new HikariDataSource(config);
    }

    private Path resolveDatabasePath(String rawPath) {
        if (rawPath.startsWith("jdbc:sqlite:")) {
            return Paths.get(rawPath.substring("jdbc:sqlite:".length()));
        }
        Path candidate = Paths.get(rawPath);
        if (candidate.isAbsolute()) {
            return candidate;
        }
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if ("../database/database.sqlite".equals(rawPath) && Files.isDirectory(cwd.resolve("backend-java"))) {
            return cwd.resolve("database").resolve("database.sqlite");
        }
        return cwd.resolve(candidate).normalize();
    }
}
