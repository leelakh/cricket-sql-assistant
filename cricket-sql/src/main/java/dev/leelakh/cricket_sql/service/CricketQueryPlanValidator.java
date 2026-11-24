
package dev.leelakh.cricket_sql.service;

import dev.leelakh.cricket_sql.model.CricketQueryPlan;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CricketQueryPlanValidator {

    private static final Set<String> SUPPORTED_METRICS =
            Set.of("RUNS");

    private static final Set<String> SUPPORTED_ENTITIES =
            Set.of("BATTER");

    private static final Set<String> SUPPORTED_SCOPES =
            Set.of("SEASON");

    private static final Set<String> SUPPORTED_AGGREGATIONS =
            Set.of("SUM");

    private static final Set<String> SUPPORTED_COMPARISONS =
            Set.of("GREATER_THAN", "LESS_THAN");

    private static final Set<String> SUPPORTED_SORT_DIRECTIONS =
            Set.of("ASC", "DESC");

    private static final Set<String> SUPPORTED_GROUP_BY =
            Set.of("season", "batter");

    public void validate(CricketQueryPlan plan) {

        if (plan == null) {
            throw new IllegalArgumentException(
                    "AI did not generate a query plan"
            );
        }

        requireSupported("metric", plan.metric(), SUPPORTED_METRICS);
        requireSupported("entity", plan.entity(), SUPPORTED_ENTITIES);
        requireSupported("scope", plan.scope(), SUPPORTED_SCOPES);
        requireSupported(
                "aggregation",
                plan.aggregation(),
                SUPPORTED_AGGREGATIONS
        );

        if (plan.comparison() == null) {
            if (plan.threshold() != null) {
                throw new IllegalArgumentException(
                        "Threshold must be null when comparison is not specified"
                );
            }
        } else {
            requireSupported(
                    "comparison",
                    plan.comparison(),
                    SUPPORTED_COMPARISONS
            );

            if (plan.threshold() == null || plan.threshold() < 0) {
                throw new IllegalArgumentException(
                        "A valid threshold is required for the comparison"
                );
            }
        }

        if (plan.limit() == null
                || plan.limit() < 1
                || plan.limit() > 100) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 100"
            );
        }

        requireSupported(
                "sort direction",
                plan.sortDirection(),
                SUPPORTED_SORT_DIRECTIONS
        );

        if (plan.groupBy() == null
                || plan.groupBy().size() != 2
                || plan.groupBy().contains(null)
                || !Set.copyOf(plan.groupBy()).equals(SUPPORTED_GROUP_BY)) {
            throw new IllegalArgumentException(
                    "Season batting queries must group by season and batter"
            );
        }

        if (plan.season() != null && plan.season().isBlank()) {
            throw new IllegalArgumentException(
                    "Season cannot be blank"
            );
        }

        if (plan.player() != null && plan.player().isBlank()) {
            throw new IllegalArgumentException(
                    "Player cannot be blank"
            );
        }

        if (plan.minimumBalls() != null && plan.minimumBalls() < 0) {
            throw new IllegalArgumentException(
                    "Minimum balls cannot be negative"
            );
        }
    }

    private void requireSupported(
            String field,
            String value,
            Set<String> supportedValues
    ) {
        if (value == null || !supportedValues.contains(value)) {
            throw new IllegalArgumentException(
                    "Unsupported or missing " + field + ": " + value
            );
        }
    }
}
