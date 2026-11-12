# REST API Documentation — College Event Management System

**Base URL (Backend):** `http://localhost:5000/api`  
**Frontend Proxy URL:** `http://localhost:3000/api` (proxied by Vite to `http://localhost:5000`)

All protected routes require an HTTP `Authorization` header:

```text
Authorization: Bearer <token>
```

JWT tokens are issued by `/api/auth/register` and `/api/auth/login` and remain valid for 48 hours (`172800000` ms).

---

## 1. Health Check

### `GET /api/health`
Simple liveness check (no authentication required).

**Response `200 OK`**
```json
{
  "status": "ok",
  "service": "college-event-platform-backend"
}
```

---

## 2. Authentication

### `POST /api/auth/register`
Register a new user (`admin`, `student`, or `faculty`; defaults to `student` if omitted or unrecognized).  
- `faculty` registrations require a valid unused `facultyId`.
- `admin` registrations require a valid unused `adminId`.

**Request Body**
```json
{
  "name": "Jane Doe",
  "email": "jane@college.edu",
  "password": "secret123",
  "role": "student"
}
```

**Response `201 Created`**
```json
{
  "token": "<jwt-token>",
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@college.edu",
    "role": "student"
  }
}
```

**Error Responses**
- `400 Bad Request`: `{ "error": "name, email and password are required" }`, `{ "error": "Invalid Faculty ID. Please enter a valid Faculty ID." }`, or `{ "error": "Invalid Admin ID. Please enter a valid Admin ID." }`
- `409 Conflict`: `{ "error": "Email already registered" }`, `{ "error": "This Faculty ID has already been registered." }`, or `{ "error": "This Admin ID has already been registered." }`

---

### `POST /api/auth/login`
Authenticate an existing user and receive a JWT token.

**Request Body**
```json
{
  "email": "jane@college.edu",
  "password": "secret123"
}
```

**Response `200 OK`**
```json
{
  "token": "<jwt-token>",
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@college.edu",
    "role": "student"
  }
}
```

**Error Responses**
- `400 Bad Request`: `{ "error": "email and password are required" }`
- `401 Unauthorized`: `{ "error": "Invalid credentials" }`

---

## 3. Events

### `GET /api/events`
List all events ordered chronologically by `date ASC`.  
**Access:** Any authenticated user (`admin`, `student`, `faculty`).

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "title": "Tech Fest",
    "description": "Annual college technology festival",
    "location": "Main Auditorium",
    "date": "2026-09-15T10:00:00",
    "capacity": 200,
    "createdBy": 1,
    "organizer": {
      "id": 1,
      "name": "Admin User",
      "email": "admin@college.edu"
    },
    "createdAt": "2026-08-01T09:00:00",
    "updatedAt": "2026-08-01T09:00:00"
  }
]
```

---

### `GET /api/events/{id}`
Retrieve details for a single event.  
**Access:** Any authenticated user.

**Response `200 OK`** — Same object shape as an item in `GET /api/events`.  
**Error `404 Not Found`** — `{ "error": "Event not found" }`

---

### `POST /api/events`
Create a new event.  
**Access:** `admin` only.

**Request Body**
```json
{
  "title": "Tech Fest",
  "description": "Annual college technology festival",
  "location": "Main Auditorium",
  "date": "2026-09-15T10:00",
  "capacity": 200
}
```

**Response `201 Created`** — Created event object.  
**Error `403 Forbidden`** — `{ "error": "Forbidden: insufficient role" }`

---

### `PUT /api/events/{id}`
Update an existing event.  
**Access:** `admin` only.

**Request Body**
```json
{
  "title": "Tech Fest 2026",
  "description": "Updated schedule",
  "location": "Main Auditorium",
  "date": "2026-09-15T11:00",
  "capacity": 250
}
```

**Response `200 OK`** — Updated event object.

---

### `DELETE /api/events/{id}`
Delete an event and its associated registrations.  
**Access:** `admin` only.

**Response `200 OK`**
```json
{
  "message": "Event deleted"
}
```

---

## 4. Registrations

### `POST /api/registrations/{eventId}`
Register the currently authenticated student for an event.  
**Access:** `student` only.

**Response `201 Created`**
```json
{
  "id": 1,
  "eventId": 1,
  "userId": 2,
  "status": "registered",
  "createdAt": "2026-08-02T14:30:00",
  "updatedAt": "2026-08-02T14:30:00"
}
```

**Error Responses**
- `400 Bad Request`: `{ "error": "Event is at full capacity" }`
- `404 Not Found`: `{ "error": "Event not found" }`
- `409 Conflict`: `{ "error": "Already registered for this event" }`

---

### `DELETE /api/registrations/{eventId}`
Cancel the current student's active registration for an event (transitions status to `cancelled`).  
**Access:** `student` only.

**Response `200 OK`**
```json
{
  "message": "Registration cancelled"
}
```

**Error `404 Not Found`** — `{ "error": "Registration not found" }`

---

### `GET /api/registrations/my`
List all registrations for the current student, including nested `Event` details.  
**Access:** `student` only.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "eventId": 1,
    "userId": 2,
    "status": "registered",
    "Event": {
      "id": 1,
      "title": "Tech Fest",
      "description": "Annual college technology festival",
      "location": "Main Auditorium",
      "date": "2026-09-15T10:00:00",
      "capacity": 200
    },
    "createdAt": "2026-08-02T14:30:00",
    "updatedAt": "2026-08-02T14:30:00"
  }
]
```

---

## 5. Reports

### `GET /api/reports/participation`
Participation summary for all events including registered count, attended count, and fill rate percentage.  
**Access:** `faculty` or `admin`.

**Response `200 OK`**
```json
[
  {
    "eventId": 1,
    "title": "Tech Fest",
    "date": "2026-09-15T10:00:00",
    "capacity": 200,
    "registeredCount": 42,
    "attendedCount": 0,
    "fillRate": 21
  }
]
```

---

### `GET /api/reports/event/{id}`
Detailed participant roster for a specific event with user details.  
**Access:** `faculty` or `admin`.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "eventId": 1,
    "userId": 2,
    "status": "registered",
    "User": {
      "id": 2,
      "name": "Jane Doe",
      "email": "jane@college.edu",
      "role": "student"
    },
    "createdAt": "2026-08-02T14:30:00",
    "updatedAt": "2026-08-02T14:30:00"
  }
]
```
