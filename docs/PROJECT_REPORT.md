# System Architecture & Project Report

## 1. Problem Statement
Colleges regularly host seminars, hackathons, cultural festivals, workshops, and academic conferences across multiple departments. Managing these events manually using spreadsheets or paper sign-up sheets leads to overbooking, lack of real-time capacity tracking, scattered participant lists, and difficulty generating attendance and participation reports for faculty and administrators.

The **College Event Management System** provides a centralized web platform where administrators organize and manage events, students discover and register for events within enforced capacity limits, and faculty/administrators monitor participation metrics in real time.

## 2. Objectives
- Provide role-based authentication and authorization (`admin`, `student`, `faculty`) using stateless JWT security.
- Allow administrators to create, edit, and delete campus events with capacity limits and scheduling details.
- Enable students to browse upcoming events, register with automatic duplicate and capacity checks, view personal registrations, and cancel registrations.
- Provide faculty and administrators with participation analytics (registered counts, attended counts, capacity fill rates, and event rosters).
- Persist all data reliably in a zero-configuration local SQLite database (`database/database.sqlite`) using Spring Data JPA / Hibernate.

## 3. System Architecture

```text
+-------------------------------------------------------------------+
|                        React Frontend (Vite)                      |
|                     http://localhost:3000                         |
|  - Pages: Login, Register, Dashboard, Events, EventForm,          |
|           MyRegistrations, Reports                                |
|  - State: AuthContext + Axios (/api/*)                            |
+---------------------------------+---------------------------------+
                                  | HTTP/JSON (Vite dev proxy -> :5000)
                                  v
+-------------------------------------------------------------------+
|                  Java Spring Boot Backend (Port 5000)             |
|                                                                   |
|  Security Layer: Spring Security + SecurityConfig.JwtHelper        |
|  Controller Layer: Auth, Event, Registration Controllers           |
|  Service Layer: AuthService, EventService, RegistrationService    |
|  Repository Layer: Spring Data JPA Repositories                   |
|  ORM / Dialect: Hibernate 6 + SQLiteDialect + HikariCP (size=1)   |
+---------------------------------+---------------------------------+
                                  | SQLite JDBC (org.xerial:sqlite-jdbc)
                                  v
+-------------------------------------------------------------------+
|                      SQLite Database File                         |
|                   database/database.sqlite                        |
|  Tables: Users, Events, Registrations                             |
+-------------------------------------------------------------------+
```

## 4. Modules & Implementation Details

### 4.1 Authentication & Security Module
- **Password Hashing:** `BCryptPasswordEncoder` (strength 10) stores salted password hashes in `Users.password`.
- **Stateless JWT:** `SecurityConfig.JwtHelper` signs HMAC-SHA256 tokens (`io.jsonwebtoken:jjwt`) containing `id`, `email`, and `role` claims with a 48-hour expiry.
- **Role-Based Access Control (RBAC):** `SecurityConfig` and `@PreAuthorize` enforce endpoint permissions (`ROLE_ADMIN`, `ROLE_STUDENT`, `ROLE_FACULTY`) with consistent JSON `401` and `403` error payloads.

### 4.2 Event Management Module
- **Controllers & Services:** `EventController` and `EventService` manage listing, retrieval, creation, updating, and deletion.
- **Validation & Date Handling:** Accepts both `YYYY-MM-DDTHH:mm` (from HTML `datetime-local` inputs) and ISO-8601 date strings, returning normalized timestamps and organizer details.

### 4.3 Registration & Capacity Enforcement Module
- **Duplicate Guard:** Prevents a student from registering twice while an active `registered` record exists (`409 Conflict`).
- **Capacity Guard:** Counts active `registered` records against `Events.capacity` before confirming registration (`400 Bad Request` when full).
- **Cancellation:** Updates status from `registered` to `cancelled`, freeing capacity for other students and allowing re-registration if desired.

### 4.4 Reporting Module
- **Participation Summary:** Aggregates `registeredCount`, `attendedCount`, and computed `fillRate` percentage across all events for `faculty` and `admin` roles.
- **Event Roster:** Returns all registration entries with embedded `User` profiles (`id`, `name`, `email`, `role`) for a given event.

## 5. Technologies Used
- **Frontend:** React 18, React Router DOM 6, Axios, Vite 5
- **Backend:** Java 17, Spring Boot 3.3.5, Spring Web, Spring Security, Spring Data JPA, Hibernate 6, Bean Validation, JJWT 0.12.6
- **Database:** SQLite 3 (`org.xerial:sqlite-jdbc` + `hibernate-community-dialects`)
- **Build & Testing:** Maven, JUnit 5, Spring MockMvc

## 6. Running the Complete Project

### Prerequisites
- Java 17+ (`java -version`)
- Apache Maven 3.8+ (`mvn -version`)
- Node.js 18+ and npm (`node -v`, `npm -v`)

### Step 1: Start the Java Spring Boot Backend
```bash
cd backend-java
mvn spring-boot:run
```
The API server starts on `http://localhost:5000` and automatically initializes `../database/database.sqlite`.

### Step 2: Start the React Frontend
Open a second terminal:
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:3000` in your browser. Vite proxies all `/api/*` requests to `http://localhost:5000`.

### Step 3: Run Backend Integration Tests
```bash
cd backend-java
mvn test
```
