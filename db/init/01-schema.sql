CREATE TABLE matches (
                         match_id        text PRIMARY KEY,
                         season          text,
                         match_date      date,
                         venue           text,
                         city            text,
                         team1           text,
                         team2           text,
                         toss_winner     text,
                         toss_decision   text,
                         winner          text,
                         win_by_runs     int,
                         win_by_wickets  int,
                         player_of_match text
);

CREATE TABLE deliveries (
                            id            bigserial PRIMARY KEY,
                            match_id      text REFERENCES matches(match_id),
                            innings       int,
                            batting_team  text,
                            over_no       int,
                            ball_in_over  int,
                            batter        text,
                            bowler        text,
                            non_striker   text,
                            batter_runs   int,
                            extras        int,
                            total_runs    int,
                            wicket_kind   text,
                            player_out    text
);
CREATE INDEX ON deliveries(match_id);
CREATE INDEX ON deliveries(batter);
CREATE INDEX ON deliveries(bowler);