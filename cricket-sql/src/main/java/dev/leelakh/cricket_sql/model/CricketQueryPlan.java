
package dev.leelakh.cricket_sql.model;

import java.util.List;

public record CricketQueryPlan(
        String metric,
        String entity,
        String scope,
        String aggregation,
        List<String> groupBy,
        String comparison,
        Integer threshold,
        Integer minimumBalls,
        Integer limit,
        String sortDirection,
        String season,
        String player
) {
}
