# EchoRoom — Real-Time Room-Based Chat Application

A self-hostable chat application with owner-approval rooms, JWT-secured
messaging over WebSocket/STOMP, and an admin dashboard — implemented per the
project synopsis (Project ID: Cs3I_09).

## Stack

| Layer          | Technology                                              |
|----------------|-----------------------------------------------------------|
| Frontend       | React 18, Vite, Tailwind CSS, @stomp/stompjs, axios       |
| Backend        | Java 17, Spring Boot 3.4.1, Spring Security, Spring WebSocket |
| Database       | MongoDB 5.0                                                |
| Real-time      | WebSocket over the STOMP sub-protocol                      |
| Auth           | JWT (JJWT) + BCrypt password hashing                        |
| Containers     | Docker, Docker Compose                                      |

## Project layout

```
echoroom-app/
├── backend/                Spring Boot REST + WebSocket API
│   ├── src/main/java/com/echoroom/
│   │   ├── config/         Security, WebSocket, CORS, admin bootstrap
│   │   ├── security/       JWT util, REST filter, STOMP auth interceptor
│   │   ├── model/          User, Room, Message (MongoDB documents)
│   │   ├── repository/     Spring Data MongoDB repositories
│   │   ├── dto/            Request/response payloads
│   │   ├── service/        Business logic (users, rooms, chat, admin)
│   │   ├── controller/     REST + STOMP (@MessageMapping) endpoints
│   │   └── exception/      Centralized error handling
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                React SPA
│   ├── src/
│   │   ├── api/             axios client (attaches JWT automatically)
│   │   ├── context/         AuthContext (login/register/logout)
│   │   ├── socket/           STOMP client wrapper
│   │   ├── pages/            Login, Register, Rooms, ChatRoom, AdminDashboard
│   │   └── components/       NavBar, Avatar, MessageBubble, TypingIndicator, JoinRequests
│   ├── nginx.conf            Production reverse proxy (API + WS)
│   ├── vite.config.js / tailwind.config.js
│   └── Dockerfile
└── docker-compose.yml         Orchestrates mongo + backend + frontend
```

## Quick start — Docker Compose (recommended)

Requires only Docker + Docker Compose installed.

```bash
cd echoroom-app
docker compose up --build
```

- Frontend: http://localhost
- Backend API: http://localhost:8080/api
- MongoDB: localhost:27017 (container-internal for the app)

On first boot the backend automatically creates an administrator account:

```
username: admin
password: admin123
```

**Change these immediately** by setting `ADMIN_USERNAME` / `ADMIN_PASSWORD`
env vars (or a `.env` file at the repo root) before the first run, and
change `JWT_SECRET` to a long random value for anything beyond local testing.

```bash
# .env (create at repo root, next to docker-compose.yml)
JWT_SECRET=replace-with-a-long-random-string
ADMIN_USERNAME=youradminname
ADMIN_PASSWORD=a-strong-password
```

Log in as `admin` and open the **Admin Dashboard** link in the navbar to see
room stats and close rooms.

## Running locally without Docker

### 1. MongoDB
Run a local MongoDB instance on `mongodb://localhost:27017/echoroom`, or
point `MONGO_URI` at any reachable instance (e.g. MongoDB Atlas).

### 2. Backend
```bash
cd backend
# Requires JDK 17 and Maven
mvn spring-boot:run
```
The API starts on `http://localhost:8080`. Key env vars (all optional,
sensible defaults in `application.yml`):

```
MONGO_URI, JWT_SECRET, JWT_EXPIRATION_MS, CORS_ORIGINS,
ADMIN_USERNAME, ADMIN_PASSWORD, UPLOAD_DIR
```

### 3. Frontend
```bash
cd frontend
cp .env.example .env      # points at http://localhost:8080
npm install
npm run dev
```
Open `http://localhost:5173`.

## How the core features map to the code

- **Owner-approval rooms** — `RoomService.requestJoin/approveJoin/denyJoin`,
  surfaced live to the owner via `/user/queue/room-events` and to the UI in
  `JoinRequests.jsx`.
- **Real-time messaging** — `ChatController` (`@MessageMapping("/room/{roomId}/send")`)
  broadcasts to `/topic/room/{roomId}`; the frontend subscribes in `ChatRoom.jsx`.
- **Typing indicators, presence, read receipts** — dedicated STOMP destinations
  (`/topic/room/{roomId}/typing`, `/presence`) and `ChatService.markRead`.
- **Reactions, edit, delete** — `ChatService` + corresponding STOMP handlers;
  only the sender may edit, sender or room owner may delete.
- **Identity & access security** — every REST call is authorized via
  `JwtAuthFilter`; every STOMP CONNECT is authorized via
  `StompAuthChannelInterceptor`. Handlers always use the server-derived
  `Principal`/`Authentication` name, never a client-supplied username field.
- **Admin dashboard** — `AdminController`/`AdminService`, gated by
  `hasRole("ADMIN")` in `SecurityConfig`, rendered in `AdminDashboard.jsx`.
- **File/image sharing** — `FileController` validates extension/size and
  stores to a Docker volume; messages reference the returned `/uploads/...` URL.

## Notes & next steps for a production deployment

- Put the app behind HTTPS/WSS (e.g. a TLS-terminating reverse proxy in front
  of the `frontend` nginx container) — the code already assumes `wss://` in
  production since `VITE_WS_URL`/`app.cors.allowed-origins` are configurable.
- Consider moving very high-volume rooms from embedded `Room.messages` arrays
  to a separate `messages` collection with pagination if message volume grows
  large; the current design favors simplicity for typical room sizes.
- Add rate limiting on `/api/auth/**` and `/api/files/upload` before exposing
  publicly.
- This code has been carefully reviewed but not build-verified in a live
  Maven/npm environment — run `mvn -q verify` and `npm run build` locally
  before deploying, and fix any straggling issues that surface.
