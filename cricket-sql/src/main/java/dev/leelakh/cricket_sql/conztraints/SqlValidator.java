
package dev.leelakh.cricket_sql.conztraints;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SqlValidator {

    private static final Pattern ALLOWED_TABLES =
            Pattern.compile("\\b(matches|deliveries)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern FORBIDDEN_KEYWORDS =
            Pattern.compile(
                    "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|"
                            + "GRANT|REVOKE|COPY|CALL|DO|EXECUTE|"
                            + "SET|RESET|VACUUM|ANALYZE)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    public String validate(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }

        String cleanedSql = sql.trim();

        if (!cleanedSql.toUpperCase(Locale.ROOT).startsWith("SELECT")
                && !cleanedSql.toUpperCase(Locale.ROOT).startsWith("WITH")) {
            throw new IllegalArgumentException(
                    "Only SELECT queries are allowed"
            );
        }

        String withoutTrailingSemicolon =
                cleanedSql.replaceFirst(";\\s*$", "");

        if (withoutTrailingSemicolon.contains(";")) {
            throw new IllegalArgumentException(
                    "Multiple SQL statements are not allowed"
            );
        }

        if (FORBIDDEN_KEYWORDS.matcher(cleanedSql).find()) {
            throw new IllegalArgumentException(
                    "Forbidden SQL keyword detected"
            );
        }

        if (!ALLOWED_TABLES.matcher(cleanedSql).find()) {
            throw new IllegalArgumentException(
                    "Query must use matches or deliveries"
            );
        }

        return withoutTrailingSemicolon;
    }
}