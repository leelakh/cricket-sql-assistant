package dev.leelakh.cricket_sql;

import org.springframework.boot.SpringApplication;

public class TestCricketSqlApplication {

	public static void main(String[] args) {
		SpringApplication.from(CricketSqlApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
