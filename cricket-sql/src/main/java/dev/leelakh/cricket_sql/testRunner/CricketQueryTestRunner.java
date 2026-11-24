
package dev.leelakh.cricket_sql.testRunner;

import dev.leelakh.cricket_sql.service.CricketQueryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("test-query")
public class CricketQueryTestRunner implements CommandLineRunner {

    private final CricketQueryService cricketQueryService;

    public CricketQueryTestRunner(
            CricketQueryService cricketQueryService
    ) {
        this.cricketQueryService = cricketQueryService;
    }

    @Override
    public void run(String... args) {

        System.out.println("\n========== TOP 10 IPL RUN SCORERS ==========");

        List<Map<String, Object>> runScorers =
                cricketQueryService.getTopRunScorers(10);

        for (Map<String, Object> result : runScorers) {
            System.out.println(
                    result.get("batter") + " - "
                            + result.get("total_runs") + " runs"
            );
        }

        System.out.println("\n========== TOP 10 IPL WICKET TAKERS ==========");

        List<Map<String, Object>> wicketTakers =
                cricketQueryService.getTopWicketTakers(10);

        for (Map<String, Object> result : wicketTakers) {
            System.out.println(
                    result.get("bowler") + " - "
                            + result.get("total_wickets") + " wickets"
            );
        }

        System.out.println("\n========== TEST RUNNER COMPLETED ==========");
    }
}
