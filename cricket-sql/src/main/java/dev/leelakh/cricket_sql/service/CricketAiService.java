
package dev.leelakh.cricket_sql.service;

import dev.leelakh.cricket_sql.model.CricketQueryPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class CricketAiService {

    private static final Logger logger =
            LoggerFactory.getLogger(CricketAiService.class);

    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    public CricketAiService(
            OllamaService ollamaService,
            ObjectMapper objectMapper
    ) {
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
    }

    public CricketQueryPlan generateQueryPlan(String question) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be empty"
            );
        }

        String prompt = """
                You are an IPL cricket analytics query planner.

                Convert the user's question into one JSON query plan.

                RULES:
                - Return valid JSON only.
                - Do not generate SQL.
                - Do not explain anything.
                - Do not include Markdown.
                - Never invent values.
                - Use null for optional fields that are not required.
                - Extract the actual season and player from the question.
                - Do not ignore player names or season filters.

                SUPPORTED VALUES:

                metric: RUNS
                entity: BATTER
                scope: SEASON
                aggregation: SUM

                comparison:
                GREATER_THAN
                LESS_THAN
                null

                groupBy:
                season
                batter

                sortDirection:
                ASC
                DESC

                FIELD RULES:

                1. metric must be RUNS.
                2. entity must be BATTER.
                3. scope must be SEASON.
                4. aggregation must be SUM.

                5. For a question asking for a player's runs
                   in a particular season:
                   - groupBy must be ["season", "batter"]
                   - comparison must be null
                   - threshold must be null
                   - extract the season into season
                   - extract the player's name into player

                6. For a question asking which players scored
                   more than a number of runs in a season:
                   - comparison must be GREATER_THAN
                   - threshold must contain that number
                   - groupBy must be ["season", "batter"]
                   - season must be null if no specific season
                     is mentioned
                   - player must be null

                7. For a question asking for fewer than a
                   specified number of runs:
                   - comparison must be LESS_THAN
                   - threshold must contain that number

                8. If no season is specified, season must be null.

                9. If no player is specified, player must be null.

                10. If no minimum balls requirement is specified,
                    minimumBalls must be null.

                11. If no result limit is specified, limit must be 100.

                12. Use DESC for highest runs and ASC for lowest runs.

                13. Do not assume a comparison when the question
                    simply asks how many runs a player scored.

                EXAMPLE 1:

                Question:
                How many runs did Virat Kohli score in 2026 season
                of IPL?

                JSON:
                {
                    "metric": "RUNS",
                    "entity": "BATTER",
                    "scope": "SEASON",
                    "aggregation": "SUM",
                    "groupBy": ["season", "batter"],
                    "comparison": null,
                    "threshold": null,
                    "minimumBalls": null,
                    "limit": 100,
                    "sortDirection": "DESC",
                    "season": "2026",
                    "player": "Virat Kohli"
                }

                EXAMPLE 2:

                Question:
                Which player has scored more than 700 in a
                single season of IPL so far?

                JSON:
                {
                    "metric": "RUNS",
                    "entity": "BATTER",
                    "scope": "SEASON",
                    "aggregation": "SUM",
                    "groupBy": ["season", "batter"],
                    "comparison": "GREATER_THAN",
                    "threshold": 700,
                    "minimumBalls": null,
                    "limit": 100,
                    "sortDirection": "DESC",
                    "season": null,
                    "player": null
                }

                EXAMPLE 3:

                Question:
                Which players scored fewer than 500 runs
                in a single IPL season?

                JSON:
                {
                    "metric": "RUNS",
                    "entity": "BATTER",
                    "scope": "SEASON",
                    "aggregation": "SUM",
                    "groupBy": ["season", "batter"],
                    "comparison": "LESS_THAN",
                    "threshold": 500,
                    "minimumBalls": null,
                    "limit": 100,
                    "sortDirection": "ASC",
                    "season": null,
                    "player": null
                }

                USER QUESTION:
                """ + question;

        String response = ollamaService.ask(prompt);

        logger.info("Raw Ollama query plan response: {}", response);

        return parseQueryPlan(response);
    }

    private CricketQueryPlan parseQueryPlan(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "Ollama returned an empty query plan"
            );
        }

        String json = response.trim();

        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new IllegalStateException(
                    "Ollama did not return a valid JSON query plan: "
                            + response
            );
        }

        json = json.substring(start, end + 1);

        try {
            return objectMapper.readValue(
                    json,
                    CricketQueryPlan.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to parse the AI query plan: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}
