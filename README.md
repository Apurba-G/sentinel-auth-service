# E-Commerce Auth & Identity Microservice (`ecommerce-auth-service`)

Enterprise Spring Boot 3 Authentication and User Identity Microservice powered by PostgreSQL 16 in Docker, MinIO Object Storage, Flyway DB migrations, JJWT 0.12.5, Swagger OpenAPI 3 UI, and automated CI/CD scripts.

---

## 1. Docker & Docker Compose Setup

Run PostgreSQL 16 and MinIO in Docker containers with one simple command:

```bash
# Start PostgreSQL (Port 5432), MinIO (Ports 9000 & 9001), and Auth Service (Port 8081)
docker-compose up --build -d
```

### Dockerized Services:
- **`auth-postgres-db`**: PostgreSQL 16 database running at `localhost:5432` (`auth_db`).
- **`auth-minio-storage`**: MinIO S3 Object Storage API at `localhost:9000` & Web Console UI at `http://localhost:9001`.
- **`ecommerce-auth-service`**: Microservice running at `http://localhost:8081`.

---

## 2. Interactive Swagger UI API Testing

Once the service is running, open Swagger UI in your browser:
- **Swagger UI Interactive URL**: `http://localhost:8081/swagger-ui.html`
- **OpenAPI 3 JSON Docs**: `http://localhost:8081/v3/api-docs`

---

## 3. Automation Scripts

### Automated Test Runner (`run-tests.bat` / `run-tests.sh`)
Executes Maven test suites, validates compilation, and reports test results:
```bash
# Windows
run-tests.bat

# Linux / Mac
chmod +x run-tests.sh && ./run-tests.sh
```

### Automated Git Commit & Push (`git-push.bat` / `git-push.sh`)
Automatically stages all modified files, commits with a timestamped or custom message, and pushes to remote Git:
```bash
# Windows (Default timestamp message)
git-push.bat

# Windows (Custom commit message)
git-push.bat "Feature: Added Docker Compose and Swagger UI"

# Linux / Mac
chmod +x git-push.sh && ./git-push.sh "Feature: Added Docker Compose and Swagger UI"
```

---

## 4. File Structure

```
ecommerce-auth-service/
├── Dockerfile                                # Multi-stage Docker build
├── docker-compose.yml                        # Postgres 16 + MinIO + Auth Service containers
├── .dockerignore                             # Docker build ignore rules
├── .gitignore                                # Git ignore rules
├── run-tests.bat / run-tests.sh              # Automated Test Runner scripts
├── git-push.bat / git-push.sh                # Automated Git Commit & Push scripts
├── pom.xml                                   # Spring Boot 3 + Swagger dependencies
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/auth/
│   │   │   ├── AuthServiceApplication.java
│   │   │   ├── config/                       # SwaggerConfig, SecurityConfig, ApplicationConfig
│   │   │   ├── controller/                   # AuthController, UserController (Swagger Annotated)
│   │   │   ├── dto/                          # RegisterRequest, LoginRequest, AddressRequest, AuthResponse
│   │   │   ├── entity/                       # User, Role, Address, RefreshToken
│   │   │   ├── exception/                    # GlobalExceptionHandler, Custom Exceptions
│   │   │   ├── filter/                       # JwtAuthenticationFilter
│   │   │   ├── repository/                   # UserRepository, RoleRepository, AddressRepository
│   │   │   └── service/                      # AuthService, UserService, JwtService, Impl
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/V1__init_auth.sql # 9-Table Flyway DB Migration & Seed Data
│   └── test/                                 # JUnit 5 & MockMvc unit tests
```
