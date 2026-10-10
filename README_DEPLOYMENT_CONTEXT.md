# Patrol Tracker - Project Structure & Deployment Status

This document explains the current state, architecture, and deployment blockers for the **Patrol Tracker** application. It is designed to be fed into an LLM to quickly establish context and find a solution for hosting.

## 1. Project Architecture
- **Backend:** Java 17, Spring Boot 3.2.3, Spring Data JPA
- **Frontend / UI:** Thymeleaf templates (Server-Side Rendered by Spring Boot), HTML/CSS/JS
- **Database:** Supabase (PostgreSQL), accessed via JDBC (`org.postgresql.Driver`)
- **Containerization:** A multi-stage `Dockerfile` is included to build and run the Spring Boot `.jar`.

## 2. The Current Deployment Strategy
We are using a **Hybrid / Proxy Architecture**:
1. **Vercel (Frontend Proxy):** We want to use Vercel for our custom domain and frontend caching. A `vercel.json` file is configured with `rewrites` to proxy all traffic (`/(.*)`) to the backend server.
2. **Backend Server (THE MISSING PIECE):** We need a platform to host the Spring Boot Docker container that renders the Thymeleaf templates and serves the API. 
3. **Database:** Hosted successfully on Supabase.

## 3. The Core Blocker & Problem
We need to find a **free (or highly accessible) platform to host the Java Spring Boot backend**.
We have exhausted the following options:
- **Vercel Natively:** Cannot be used to host the backend because Vercel only supports Node.js/Python/Go serverless functions, not Java/Spring Boot.
- **Render:** Abandoned. (Suffers from aggressive free-tier spin-downs and initial JDBC configuration headaches).
- **Koyeb:** Abandoned. (Recently acquired by Mistral AI, dashboard gated/disabled for new users).
- **Railway:** Abandoned. (The free trial period expired).

**The Question for the LLM:** 
"Given this Java Spring Boot + Thymeleaf codebase, what is the best free alternative to host a Docker container or Java application right now? Alternatively, what is the easiest path to migrate this so it can run 100% natively on Vercel?"

## 4. Codebase Structure
The repository is organized as a standard Maven Spring Boot project:

```text
/
├── pom.xml                        # Maven dependencies (Spring Web, JPA, Thymeleaf, Postgres, H2)
├── Dockerfile                     # Multi-stage build (Maven build -> Eclipse Temurin JRE 17)
├── vercel.json                    # Vercel proxy configuration (rewrites traffic to the backend)
└── src/main/
    ├── java/com/patroltracker/
    │   ├── PatrolTrackerApplication.java  # Main entry point
    │   ├── config/
    │   │   ├── DataInitializer.java       # Seeds initial DB data (users, checkpoints, logs)
    │   │   └── WebCorsConfig.java         # CORS configuration to allow Vercel proxy
    │   ├── controller/
    │   │   ├── PatrolWebController.java   # Serves Thymeleaf HTML pages
    │   │   └── PatrolApiController.java   # REST API for QR scans and background fetches
    │   ├── model/                         # JPA Entities (User, Checkpoint, DutyAllocation, ScanLog, etc.)
    │   ├── repository/                    # Spring Data JPA interfaces
    │   └── service/                       # Business logic (PatrolService)
    └── resources/
        ├── application.properties         # Spring Boot config (Port, DB URL, JPA settings)
        ├── static/                        # CSS, JS, and image assets
        └── templates/                     # Thymeleaf HTML files (layout, duty, users, map, checkpoints)
```

## 5. Current Database Configuration (`application.properties`)
The application is correctly configured to auto-generate tables using Hibernate and seed data automatically. 

```properties
spring.application.name=patrol-tracker
server.port=${PORT:8080}

# Supabase PostgreSQL Configuration
spring.datasource.url=${SUPABASE_DB_URL:jdbc:h2:file:./data/patroldb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1}
spring.datasource.username=${SUPABASE_DB_USER:sa}
spring.datasource.password=${SUPABASE_DB_PASSWORD:}
spring.datasource.driver-class-name=${SUPABASE_DB_DRIVER:org.h2.Driver}

# JPA Settings
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=never
```

## 6. How the app is run locally
Locally, the app compiles perfectly and runs via:
`mvn spring-boot:run` or by building the Docker container.
