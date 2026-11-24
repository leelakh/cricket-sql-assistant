
package dev.leelakh.cricket_sql;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("load")
public class CricsheetLoader implements CommandLineRunner {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${cricsheet.dir}")
    private String dir;

    public CricsheetLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        Path directory = Path.of(dir);

        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "CSV directory not found: " + directory
            );
        }

        List<Path> files;

        try (var stream = Files.list(directory)) {
            files = stream
                    .filter(path -> path.toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }

        System.out.println("Found " + files.size() + " CSV files.");

        int processed = 0;
        int failed = 0;

        for (Path file : files) {
            try {
                loadMatch(file);
                processed++;
            } catch (Exception exception) {
                failed++;

                System.err.println(
                        "Failed to load: " + file.getFileName()
                );

                exception.printStackTrace();

                throw exception;
            }

            if ((processed + failed) % 100 == 0) {
                System.out.println(
                        "Processed: " + processed + ", Failed: " + failed
                );
            }
        }

        System.out.println("Cricsheet import completed.");
        System.out.println("Successfully processed: " + processed);
        System.out.println("Failed: " + failed);
    }

    private void loadMatch(Path file) throws Exception {
        Map<String, List<List<String>>> metadata = new HashMap<>();
        List<Object[]> deliveries = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8
        );
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            for (CSVRecord record : parser) {
                if (record.size() < 2) {
                    continue;
                }

                String recordType = record.get(0);

                if ("info".equals(recordType) && record.size() >= 3) {
                    String key = record.get(1);
                    List<String> values = new ArrayList<>();

                    for (int i = 2; i < record.size(); i++) {
                        values.add(record.get(i));
                    }

                    metadata.computeIfAbsent(
                            key, ignored -> new ArrayList<>()
                    ).add(values);

                } else if ("ball".equals(recordType) && record.size() >= 9) {
                    deliveries.add(parseDelivery(record));
                }
            }
        }

        String matchIdText = firstValue(metadata, "match_id");

        if (matchIdText == null || matchIdText.isBlank()) {
            String filename = file.getFileName().toString();
            matchIdText = filename.substring(0, filename.lastIndexOf('.'));
        }

        long matchId = Long.parseLong(matchIdText);

        for (Object[] delivery : deliveries) {
            delivery[0] = matchId;
        }

        List<List<String>> teams = metadata.getOrDefault(
                "team", List.of()
        );

        String team1 = teams.size() > 0 ? teams.get(0).get(0) : null;
        String team2 = teams.size() > 1 ? teams.get(1).get(0) : null;

        String dateText = firstValue(metadata, "date");

        Date matchDate = dateText == null
                ? null
                : Date.valueOf(LocalDate.parse(dateText.replace('/', '-')));

        String metadataJson = mapper.writeValueAsString(metadata);

        jdbc.update("""
                INSERT INTO matches (
                    match_id, season, match_date, team1, team2,
                    winner, venue, city, match_type, metadata
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (match_id) DO UPDATE SET
                    season = EXCLUDED.season,
                    match_date = EXCLUDED.match_date,
                    team1 = EXCLUDED.team1,
                    team2 = EXCLUDED.team2,
                    winner = EXCLUDED.winner,
                    venue = EXCLUDED.venue,
                    city = EXCLUDED.city,
                    match_type = EXCLUDED.match_type,
                    metadata = EXCLUDED.metadata
                """,
                matchId,
                firstValue(metadata, "season"),
                matchDate,
                team1,
                team2,
                firstValue(metadata, "winner"),
                firstValue(metadata, "venue"),
                firstValue(metadata, "city"),
                firstValue(metadata, "match_type"),
                metadataJson
        );

        jdbc.update(
                "DELETE FROM deliveries WHERE match_id = ?",
                matchId
        );

        String insertSql = """
                INSERT INTO deliveries (
                    match_id, innings, over_no, ball_in_over,
                    batting_team, batter, non_striker, bowler,
                    batter_runs, extras, total_runs,
                    wicket_kind, player_out, raw_data
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """;

        for (int start = 0; start < deliveries.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, deliveries.size());

            jdbc.batchUpdate(
                    insertSql,
                    deliveries.subList(start, end)
            );
        }
    }

    private Object[] parseDelivery(CSVRecord record) throws Exception {
        String ballNotation = record.get(2);
        String[] overParts = ballNotation.split("\\.", 2);

        int overNumber = Integer.parseInt(overParts[0]) + 1;
        String ballInOver = overParts.length > 1
                ? overParts[1]
                : null;

        int batterRuns = Integer.parseInt(record.get(7));
        int extras = Integer.parseInt(record.get(8));

        String wicketKind = valueAt(record, 14);
        String playerOut = valueAt(record, 15);

        List<String> rawFields = new ArrayList<>();

        for (String field : record) {
            rawFields.add(field);
        }

        return new Object[]{
                null,
                Integer.parseInt(record.get(1)),
                overNumber,
                ballInOver,
                record.get(3),
                record.get(4),
                record.get(5),
                record.get(6),
                batterRuns,
                extras,
                batterRuns + extras,
                wicketKind,
                playerOut,
                mapper.writeValueAsString(rawFields)
        };
    }

    private static String firstValue(
            Map<String, List<List<String>>> metadata,
            String key
    ) {
        List<List<String>> values = metadata.get(key);

        if (values == null || values.isEmpty()
                || values.get(0).isEmpty()) {
            return null;
        }

        return values.get(0).get(0);
    }

    private static String valueAt(CSVRecord record, int index) {
        if (record.size() <= index || record.get(index).isBlank()) {
            return null;
        }

        return record.get(index);
    }
}