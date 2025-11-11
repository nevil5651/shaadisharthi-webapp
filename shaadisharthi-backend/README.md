### 💍 ShaadiSharthi – Java Backend

**ShaadiSharthi** is a comprehensive wedding services platform that connects `customers` with verified `service providers` while enabling `admins` to manage the ecosystem securely and efficiently.
This repository contains the backend system, built with `Java Servlets` and `MySQL`, providing a scalable, secure, and production-ready architecture.

---

## 🧠 Overview

ShaadiSharthi is a multi-role wedding services platform enabling:

- 👤 **Customers** to discover, book, and review wedding-related services
- _Customer Frontend_: `https://shaadisharthi.theworkpc.com`

- 🏢 **Service Providers** to manage profiles, services, and bookings
- **Provider Frontend**: `https://shaadisharthi.theworkpc.com/provider`

- 🛡 **Admins** to oversee users, bookings, and content moderation
- **Admin Frontend**: `https://shaadisharthi.theworkpc.com/admin`

With 58+ servlets and models, asynchronous email handling, WebSocket notifications, and layered security filters, the backend is optimized for real-world production deployment.

---

## 👥 User Roles & Features

### 👤 Customers

- Register and verify via email tokens (15-min expiry)
- Login using HttpOnly cookies for secure sessions
- Browse services with filters (category, location, price, rating)
- Book and manage services
- View booking history and post reviews

### 🏢 Service Providers

- Register services and manage profiles
- Upload images/videos using Cloudinary
- Manage bookings (accept/reject/complete)
- Monitor performance via dashboard analytics
- Update availability and pricing dynamically

### 🛡 Administrators

- Manage all users (add, edit, delete, suspend)
- Oversee bookings, reviews, and reports
- Access system logs and monitor platform activity
- Moderate content and handle disputes
- Manage role-based admin access (Super, Support, Moderator)

---

## 🛠 Tech Stack

| Layer                | Technology                                               |
| -------------------- | -------------------------------------------------------- |
| **Language**         | `Java` (JDK 11+)                                         |
| **Framework**        | `Java Servlets`                                          |
| **Server**           | Apache Tomcat 9.0                                        |
| **Database**         | `MySQL` 8.0 + `HikariCP` Connection Pooling              |
| **Authentication**   | JWT (Admin & Provider) / HttpOnly Cookies (Customer)     |
| **Async Processing** | Java `ExecutorService`                                   |
| **Email Service**    | JavaMail (Transactional Emails)                          |
| **Media Storage**    | `Cloudinary` (CDN Integration)                           |
| **Real-Time**        | `WebSocket` (Booking Notifications)                      |
| **Logging**          | `SLF4J` (Async Logging)                                  |
| **Security**         | CORS, Rate Limiting, Role Validation, Input Sanitization |

---

## 🏗️ System Architecture

The system follows a layered architecture to ensure separation of concerns and security at every step.

```
Frontend Request (Customer, Provider, Admin)
       │
       ▼
CORS Filter (Validates request origin)
       │
       ▼
Global Rate Limiter (Prevents abuse)
       │
       ▼
Authentication Filter (JWT/Session Validation)
       │
       ▼
Servlet Controller (Handles HTTP request)
       ├── DAO Layer → MySQL (via HikariCP)
       ├── Cloudinary Service (for media)
       ├── JavaMail Service (for emails, async)
       └── WebSocket Endpoint (for real-time notifications)
```

---

## 🧱 Database Schema Highlights

Core Tables:

- `customers` – Customer details and sessions
- `service_providers` – Provider details, verification data
- `admins` – Role-based admin accounts
- `services` – Service listings with category and pricing
- `bookings` – Customer-provider bookings
- `reviews` – Ratings and feedback
- `media` – Cloudinary media metadata
- `notifications` – WebSocket + email alerts

---

## 📡 Key API Endpoints

🔐 Authentication

| Method | Endpoint                | Description                                |
| ------ | ----------------------- | ------------------------------------------ |
| POST   | `/auth/register`        | Customer registration + email verification |
| POST   | `/auth/login`           | JWT-based login                            |
| POST   | `/auth/forgot-password` | Trigger password reset email               |
| POST   | `/auth/reset-password`  | Update password using reset token          |

🎪 Booking

| Method | Endpoint                | Description                    |
| ------ | ----------------------- | ------------------------------ |
| POST   | `/bookings`             | Create new booking             |
| GET    | `/bookings`             | Provider booking list          |
| POST   | `/bookings/{id}/action` | Accept/Reject/Complete booking |
| GET    | `/customer/bookings`    | Customer booking history       |

🧩 Services

| Method | Endpoint                 | Description                |
| ------ | ------------------------ | -------------------------- |
| GET    | `/services`              | List services with filters |
| GET    | `/services/{id}`         | Get service details        |
| POST   | `/services/{id}/reviews` | Add review                 |
| GET    | `/services/{id}/media`   | Retrieve media             |

🛡 Admin

| Method | Endpoint             | Description    |
| ------ | -------------------- | -------------- |
| GET    | `/admin/users`       | View all users |
| POST   | `/admin/edit-user`   | Edit user info |
| DELETE | `/admin/delete-user` | Remove user    |

---

## 🔐 Security Infrastructure

- CORS Filter – Role-based origin validation per frontend (Customer / Provider / Admin)
- Rate Limiting – Global & endpoint-specific control
- JWT Validation – Role-based token decoding and expiry checks
- Input Sanitization – SQL injection and XSS prevention
- Asynchronous Logging – Admin and error logs stored via background threads
- HttpOnly Cookies – Secure client sessions for customers

---

## ⚙️ Installation & Setup

### 🧩 Prerequisites

- Java 11+
- Apache Tomcat 9.0+
- MySQL 8.0+
- Eclipse IDE (recommended)

### 🧾 Configuration (Config.properties)

```properties
# MySQL (used to start mysql container)
MYSQL_ROOT_PASSWORD=XXXX
MYSQL_DATABASE=XXXXXXXXXXX
MYSQL_USER=XXXXX
MYSQL_PASSWORD=XXXXX

# App DB (when running in docker-compose, use mysql service host)
DB_URL=jdbc:mysql://mysql:3306/XXXXXXXXXXX
DB_USERNAME=XXXX
DB_PASSWORD=XXXXX
DB_DRIVER=com.mysql.cj.jdbc.Driver

# JWT
JWT_SECRET_KEY=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
JWT_RESET_SECRET_KEY=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

# Email
EMAIL_FROM=EXAMPLE@EXAMPLE.COM
EMAIL_PASSWORD=XXXXX XXXXX

# Cloudinary
CLOUDINARY_CLOUD_NAME=XXXXX
CLOUDINARY_API_KEY=XXXXX
CLOUDINARY_API_SECRET=XXXXX

# CORS / allowed origins
ADMIN_ALLOWED_ORIGINS=http://localhost:xxxx
SERVICEPROVIDER_ALLOWED_ORIGINS=http://localhost:4200
CUSTOMER_ALLOWED_ORIGINS=http://localhost:3000

# Role mappings
ACCOUNT_GET_ROLES=XXXXX
ACCOUNT_POST_ROLES=XXXXX
DASHBOARDSTATS_GET_ROLES=XXXXX
CHANGEPASSWORD_POST_ROLES=XXXXX
CUSTOMERS_GET_ROLES=XXXXX
CUSTOMERS_POST_ROLES=XXXXX
SERVICE_PROVIDERS_GET_ROLES=XXXXX
SERVICE_PROVIDERS_POST_ROLES=XXXXX
QUERIES_GET_ROLES=XXXXX
QUERIES_POST_ROLES=XXXXX
ADMINLOGOUT_POST_ROLES=XXXXX
UPDATE_STATUS_POST_ROLES=XXXXX
USERTRENDS_GET_ROLES=XXXXX
ORDERCOMPARISON_GET_ROLES=XXXXX
PROVIDERDASHBOARDSTATS_GET_ROLES=XXXXX
LOGOUT_POST_ROLES=XXXXX
PROVIDER_CHANGE_PASSWORD_POST_ROLES=XXXXX
PROVIDERSERVICES_GET_ROLES=XXXXX
CLOUDINARYSIGNATURE_POST_ROLES=XXXXX
CREATESERVICE_POST_ROLES=XXXXX
EDITSERVICE_PUT_ROLES=XXXXX
EDITSERVICE_POST_ROLES=XXXXX
EDITSERVICE_DELETE_ROLES=XXXXX
DELETESERVICE_DELETE_ROLES=XXXXX
BUSINESS_REGISTER_POST_ROLES=XXXXX
ADDSUPPORTQUERY_POST_ROLES=XXXXX
BOOKING_LIST_SERVLET_GET_ROLES=XXXXX
BOOKING_ACTION_POST_ROLES=XXXXX
SERVICES_GET_ROLES=CUSTOMER
REVIEWS_POST_ROLES=XXXXX
CSTMR_ACC_GET_ROLES=XXXXX
CSTMR_ACC_POST_ROLES=XXXXX
SERVICE_DETAIL_GET_ROLES=XXXXX
PROCESS_BOOKINGS_POST_ROLES=XXXXX
CSTMR_BOOKINGS_GET_ROLES=XXXXX
CSTMR_ACTION_POST_ROLES=XXXXX
CSTMR_UPCOMING_BOOKINGS_GET_ROLES=XXXXX
```

### 🚀 Deployment Steps

```bash
# 1. Clone the repository
git clone https://github.com/your-username/shaadisharthi-backend.git
cd shaadisharthi-backend

# 2. Configure database and environment
# 3. Import project into Eclipse as a Dynamic Web Project
# 4. Build WAR and deploy to Tomcat server
# 5. Access APIs at: http://localhost:8080/shaadisharthi/
```

---

## 🧩 Design Patterns Overview

| Pattern               | Purpose                                    |
| --------------------- | ------------------------------------------ |
| **DAO Pattern**       | Abstract database access layer             |
| **Filter Pattern**    | Sequential security and request processing |
| **Singleton Pattern** | Shared configuration and connection pool   |
| **Factory Pattern**   | Object creation abstraction                |
| **Observer Pattern**  | Notification handling (WebSocket + Email)  |

---

## 📈 Performance & Optimizations

- HikariCP Connection Pooling for low-latency DB access
- Indexed Queries for search and booking operations
- Pagination on all listing APIs
- Async Threads for email and logging
- JSON-based Error Handling for consistent responses

---

## 🔮 Future Enhancements

- 💳 Payment Gateway Integration (Razorpay / Stripe)
- 📊 Admin Analytics Dashboard
- 🧱 Redis Caching Implementation
- ☁️ Microservices Architecture Migration
- 📬 Message Queue for async job handling

---

## 💼 Developer Skills Demonstrated

### 🧩 Backend Engineering

- REST API design using Java Servlets
- Secure JWT & Cookie-based authentication
- MySQL schema design and optimization
- Cloudinary + JavaMail integration

### ⚙️ System Architecture

- Multi-layered architecture (Filter → Servlet → DAO → DB)
- Async services using ExecutorService
- Thread-safe design with connection pooling

---

### 🛡 Security & Scalability

- Multi-role JWT access
- Global rate limiting and CORS
- SQL/XSS protection and secure sessions

---

### 🧰 DevOps & Deployment

WAR-based deployment on Apache Tomcat

---

## 👨‍💻 Contact

- **Author**: Nevil H. — Full Stack Developer (Java + Modern JS Frameworks)
- **Email**: nevilhapaliya565188@example.com
- **GitHub**: github.com/nevil5651

---

## 📜 License

- This repository is provided for portfolio and educational purposes.

---

Built with ❤️ for the wedding industry
