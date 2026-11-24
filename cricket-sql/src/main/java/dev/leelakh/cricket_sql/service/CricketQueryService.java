
package dev.leelakh.cricket_sql.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CricketQueryService {

    private final JdbcTemplate jdbc;

    public CricketQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getTopRunScorers(int limit) {

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }

        String sql = """
                SELECT
                    batter,
                    SUM(batter_runs) AS total_runs
                FROM deliveries
                GROUP BY batter
                ORDER BY total_runs DESC, batter ASC
                LIMIT ?
                """;

        return jdbc.queryForList(sql, limit);
    }

    public List<Map<String, Object>> getTopWicketTakers(int limit) {

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }

        String sql = """
                SELECT
                    bowler,
                    COUNT(*) AS total_wickets
                FROM deliveries
                WHERE wicket_kind IN (
                    'bowled',
                    'caught',
                    'caught and bowled',
                    'lbw',
                    'stumped',
                    'hit wicket'
                )
                GROUP BY bowler
                ORDER BY total_wickets DESC, bowler ASC
                LIMIT ?
                """;

        return jdbc.queryForList(sql, limit);
    }
}
