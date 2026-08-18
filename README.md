# 🛡️ Patrol Tracker

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%20%2F%20H2-blueviolet.svg)](https://supabase.com)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

A modern, mobile-friendly **Security Patrol Tracking & Monitoring Application** built using **Spring Boot**, **Thymeleaf**, and **Supabase (PostgreSQL) / H2 Database**.

---

## 📌 Features

- **📍 Checkpoint Management**: Register and manage security checkpoints with QR codes and GPS coordinates.
- **📋 Duty Allocation**: Assign guards and patrol personnel to specific routes and time schedules.
- **📱 QR & Mobile Scanning**: Real-time checkpoint scanning and log recording.
- **🗺️ Live Map & Activity Logs**: Track patrol progress and view live scan logs on interactive maps.
- **🗃️ Archiving & Audit**: Store and manage historical patrol logs and audit data.
- **🔐 Dual Database Mode**: Runs out-of-the-box with embedded local H2 storage or integrates seamlessly with Supabase PostgreSQL cloud database.

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.3, Spring Data JPA, Hibernate
- **Frontend**: HTML5, CSS3, JavaScript, Thymeleaf Templating Engine
- **Database**: PostgreSQL (Supabase) / H2 Database
- **Build Tool**: Apache Maven (`mvnw`)

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK 17+)** installed.
- **Git** installed.

---

### 💻 Running Locally

1. **Clone the Repository**
   ```bash
   git clone https://github.com/buxarinventorymanagement-jpg/Patrol-Tracker.git
   cd Patrol-Tracker
   ```

2. **Run Application using Maven Wrapper**
   - **Windows:**
     ```cmd
     mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. **Access the Dashboard**
   Open your browser and navigate to:
   ```text
   http://localhost:8080
   ```

---

## 🗄️ Database Configuration

By default, the application runs using a local persistent **H2 database** (`./data/patroldb`).

To connect to **Supabase PostgreSQL** cloud database, set the following environment variables before running:

```bash
export SUPABASE_DB_URL="jdbc:postgresql://<YOUR_SUPABASE_HOST>:5432/postgres"
export SUPABASE_DB_USER="postgres"
export SUPABASE_DB_PASSWORD="<YOUR_SUPABASE_PASSWORD>"
export SUPABASE_DB_DRIVER="org.postgresql.Driver"
```

---

## 📁 Project Structure

```text
Patrol-Tracker/
├── src/
│   ├── main/
│   │   ├── java/com/patroltracker/
│   │   │   ├── config/          # Application configurations & Initializers
│   │   │   ├── controller/      # Web Controllers & REST API endpoints
│   │   │   ├── model/           # JPA Entities (User, Checkpoint, DutyAllocation, ScanLog, etc.)
│   │   │   ├── repository/     # Spring Data JPA Repositories
│   │   │   └── service/        # Business logic services
│   │   └── resources/
│   │       ├── static/          # CSS & JavaScript assets
│   │       ├── templates/       # Thymeleaf HTML views
│   │       ├── application.properties
│   │       └── schema-supabase.sql
├── pom.xml
└── README.md
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
