
package dev.leelakh.cricket_sql.service;

import dev.leelakh.cricket_sql.model.CricketQueryPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CricketSqlBuilder {

    public String build(CricketQueryPlan plan) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.season,
                    d.batter,
                    SUM(d.batter_runs) AS total_runs
                FROM deliveries d
                JOIN matches m
                    ON d.match_id = m.match_id
                """);

        List<String> conditions = new ArrayList<>();

        if (plan.season() != null) {
            conditions.add("m.season = ?");
        }

        if (plan.player() != null) {
            conditions.add("LOWER(d.batter) = LOWER(?)");
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ")
                    .append(String.join(" AND ", conditions))
                    .append("\n");
        }

        sql.append("""
                GROUP BY
                    m.season,
                    d.batter
                """);

        if ("GREATER_THAN".equals(plan.comparison())) {
            sql.append("HAVING SUM(d.batter_runs) > ?\n");
        } else if ("LESS_THAN".equals(plan.comparison())) {
            sql.append("HAVING SUM(d.batter_runs) < ?\n");
        }

        sql.append("ORDER BY total_runs ")
                .append(plan.sortDirection())
                .append("\n");

        sql.append("LIMIT ?");

        return sql.toString();
    }

    public List<Object> parameters(CricketQueryPlan plan) {

        List<Object> parameters = new ArrayList<>();

        if (plan.season() != null) {
            parameters.add(plan.season());
        }

        if (plan.player() != null) {
            parameters.add(plan.player());
        }

        if (plan.comparison() != null) {
            parameters.add(plan.threshold());
        }

        parameters.add(plan.limit());

        return parameters;
    }
}
