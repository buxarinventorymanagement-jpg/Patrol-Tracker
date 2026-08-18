package com.patroltracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PatrolTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatrolTrackerApplication.class, args);
        System.out.println("\n=======================================================");
        System.out.println("  PATROL TRACKER MOBILE APP STARTED SUCCESSFULLY!");
        System.out.println("  Access App UI: http://localhost:8080");
        System.out.println("  H2 Console:    http://localhost:8080/h2-console");
        System.out.println("=======================================================\n");
    }
}
