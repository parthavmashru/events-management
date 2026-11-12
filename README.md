# College Event Management System

A full-stack College Event Management Platform where **admins** create and manage campus events, **students** browse and register for events, and **faculty/admins** view participation reports and event rosters.

## Features

- **Role-Based Authentication:** User registration and login with JWT tokens and BCrypt password hashing (`admin`, `student`, `faculty`).
- **Event Management (Admin):** Create, edit, and delete events with title, description, location, date/time, and capacity limits.
- **Event Registration (Student):** Browse upcoming events, register with automatic capacity and duplicate checks, view personal registrations, and cancel registrations.
- **Participation Reports (Faculty & Admin):** View event-by-event registration counts, attended counts, capacity fill rates, and participant rosters.
- **Zero-Config SQLite Persistence:** Automatically initializes `database/database.sqlite` on startup and preserves data across restarts.

## Technology Stack

- **Frontend:** React.js (Vite), React Router, Axios
- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA / Hibernate
- **Database:** SQLite (`sqlite-jdbc` + `hibernate-community-dialects`)
- **Build Tool:** Maven (backend), npm / Vite (frontend)

## Final Folder Structure

```text
CollegeEventManagement/
├── frontend/               # React + Vite frontend application
│   ├── src/
│   │   ├── pages/          # Login, Register, Dashboard, Events, EventForm, MyRegistrations, Reports
│   │   ├── App.jsx         # Routes & ProtectedRoute
│   │   ├── AuthContext.jsx # Auth state provider
│   │   ├── Navbar.jsx      # Navigation bar
│   │   ├── api.js          # Axios API client
│   │   ├── index.css
│   │   └── main.jsx
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── backend-java/           # Java Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/events/                 # Flat package: entities, repos, DTOs, services, controllers, config
│   │   │   └── resources/application.properties
│   │   └── test/
│   │       └── java/events/CollegeEventApiIntegrationTest.java
│   └── pom.xml
├── database/               # SQLite database storage
│   └── database.sqlite     # Auto-created on backend startup
├── docs/                   # Project documentation
│   ├── API.md              # REST API reference & payloads
│   ├── ER_DIAGRAM.md       # ER diagram, schema & relationships
│   └── PROJECT_REPORT.md   # Architecture, modules & project report
└── README.md
```

## Prerequisites

- **Java JDK 17** or newer
- **Apache Maven 3.8+**
- **Node.js 18+** and **npm**

## Installation & Running Instructions

### 1. Run the Java Spring Boot Backend

```bash
cd backend-java
mvn spring-boot:run
```

- Runs on `http://localhost:5000`
- Connects to `../database/database.sqlite` (created automatically if it does not exist)

### 2. Run the React Frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

- Runs on `http://localhost:3000`
- Proxies `/api/*` requests to `http://localhost:5000`

### 3. Build for Production

```bash
# Backend JAR
cd backend-java
mvn clean package

# Frontend static bundle
cd frontend
npm run build
```

### 4. Run Backend Integration Tests

```bash
cd backend-java
mvn test
```

## SQLite Database Location

The SQLite database is stored at:

```text
database/database.sqlite
```

Configured via relative path in `backend-java/src/main/resources/application.properties` (`app.database.path=../database/database.sqlite`) so the project runs cleanly across different machines without external database servers.

## API Endpoints Overview

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/health` | Public | Health check |
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Authenticate and get JWT |
| `GET` | `/api/events` | Authenticated | List all events |
| `GET` | `/api/events/:id` | Authenticated | Get single event details |
| `POST` | `/api/events` | `admin` | Create a new event |
| `PUT` | `/api/events/:id` | `admin` | Update an event |
| `DELETE` | `/api/events/:id` | `admin` | Delete an event |
| `POST` | `/api/registrations/:eventId` | `student` | Register for an event |
| `DELETE` | `/api/registrations/:eventId` | `student` | Cancel registration |
| `GET` | `/api/registrations/my` | `student` | View own registrations |
| `GET` | `/api/reports/participation` | `faculty`, `admin` | Event participation summary |
| `GET` | `/api/reports/event/:id` | `faculty`, `admin` | Event participant roster |

For full payloads, schemas, and architecture details, see [`docs/API.md`](docs/API.md), [`docs/ER_DIAGRAM.md`](docs/ER_DIAGRAM.md), and [`docs/PROJECT_REPORT.md`](docs/PROJECT_REPORT.md).
