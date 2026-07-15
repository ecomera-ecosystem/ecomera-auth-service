# Ecomera Auth Service

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-6DB33F?logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative&logoColor=white)
[![CI](https://github.com/ecomera-ecosystem/ecomera-auth-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ecomera-ecosystem/ecomera-auth-service/actions/workflows/ci.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ecomera-auth-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ecomera-auth-service)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=ecomera-auth-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ecomera-auth-service)

Authentication and authorization microservice for the Ecomera ecosystem using JWT-based security.

---

## 📋 Overview

Handles user authentication, registration, and token management for all Ecomera microservices. Provides JWT-based stateless authentication with Redis for token blacklisting and refresh token storage.

---

## 🛠️ Tech Stack

- **Spring Boot** 3.5.11
- **Spring Security** - Authentication & authorization
- **JWT (jjwt)** 0.13.0 - Token generation and validation
- **PostgreSQL** - User data storage
- **Redis** - Token blacklist & refresh tokens
- **Liquibase** - Database migrations
- **Spring Cloud Config** - Centralized configuration
- **Eureka Client** - Service registration

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 16+ (database: `ecomera_auth`)
- Redis 7+
- Config Server running on port 8888
- Eureka Server running on port 8761

### Start the Service
```bash
mvn spring-boot:run
```

**Service available at:** `http://localhost:8081`

---

## 🔐 Authentication Flow

1. **Register:** `POST /api/auth/register`
2. **Login:** `POST /api/auth/login` → Returns access + refresh tokens
3. **Access Protected Resources:** Include `Authorization: Bearer {token}` header
4. **Refresh Token:** `POST /api/auth/refresh` → Get new access token
5. **Logout:** `POST /api/auth/logout` → Blacklist token in Redis

---

## 📡 API Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/auth/register` | POST | Register new user | ❌ |
| `/api/auth/login` | POST | Authenticate user | ❌ |
| `/api/auth/refresh` | POST | Refresh access token | ✅ (refresh token) |
| `/api/auth/logout` | POST | Invalidate token | ✅ |
| `/api/auth/whoami` | GET | Get current user info | ✅ |
| `/actuator/health` | GET | Health check | ❌ |

---

## 🔑 Token Configuration

| Token Type | Expiration | Storage |
|------------|------------|---------|
| **Access Token** | 15 minutes | Client-side only |
| **Refresh Token** | 7 days | Redis + Client-side |

**Access tokens** are short-lived for security.  
**Refresh tokens** are stored in Redis with TTL for renewal.

---

## 🗄️ Database Schema
```
app_user
├── id (UUID, PK)
├── email (unique)
├── password (BCrypt hashed)
├── first_name
├── last_name
├── role (USER, ADMIN, MANAGER)
├── created_at
└── updated_at

token_blacklist (Redis)
├── key: "blacklist:{jwt}"
├── TTL: token expiration time
```

---

## ⚙️ Configuration

Configuration fetched from **Config Server** (`auth-service.yml`):
```yaml
jwt:
  secret: {secret-key}
  expiration: 900000        # 15 min
  refresh-expiration: 604800000  # 7 days

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecomera_auth
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 🐳 Docker Support

### Build Image
```bash
docker build -t ecomera-auth-service .
```

### Run Container
```bash
docker run -p 8081:8081 \
  -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e EUREKA_SERVER_URL=http://eureka:8761/eureka/ \
  ecomera-auth-service
```

---

## 🧪 Testing
```bash
# Unit tests
mvn test

# Integration tests with Testcontainers
mvn verify
```

---

## 🔗 Related Services

**Infrastructure:**
- [Config Server](https://github.com/ecomera-ecosystem/ecomera-config-server) - Centralized configuration
- [Eureka Server](https://github.com/ecomera-ecosystem/ecomera-eureka-service-registry) - Service discovery
- [API Gateway](https://github.com/ecomera-ecosystem/ecomera-api-gateway) - Entry point (planned)

---

## 🏗️ Architecture
```
Client → API Gateway → Auth Service (port 8081)
                            ↓
                  PostgreSQL + Redis
                            ↓
                  Config Server (configs)
                            ↓
                  Eureka Server (registration)
```

---

## 📊 Security Features

✅ **BCrypt password hashing** (strength: 12)  
✅ **JWT stateless authentication**  
✅ **Token blacklist on logout** (Redis)  
✅ **Refresh token rotation**  
✅ **Role-based access control** (USER, ADMIN, MANAGER)  
✅ **CORS configuration**  
✅ **Rate limiting** (planned)

---

## 🔄 Future Enhancements

- [ ] OAuth2 social login (Google, GitHub)
- [ ] Email verification
- [ ] Password reset flow
- [ ] Two-factor authentication (2FA)
- [ ] Account lockout after failed attempts
- [ ] Audit logging

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details

---

## 👨‍💻 Maintainer

**Youssef Ammari**  
Part of the Ecomera Microservices Ecosystem

---

**Status:** 🚧 Active Development