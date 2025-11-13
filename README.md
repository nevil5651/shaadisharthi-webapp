# ShaadiSharthi — Multi-user Wedding Services Platform

**ShaadiSharthi** is a full-stack wedding services platform that connects **Customers**, **Service Providers**, and **Admins**. This repository contains the complete source code and Docker configuration to run the entire application on your local machine.

> This guide is focused on setting up a **local development environment**. The project is composed of multiple frontend applications, a Java backend, and is orchestrated using Docker Compose.

---

## Live Demo

- **Customer Portal**: [shaadisharthi.theworkpc.com](https://shaadisharthi.theworkpc.com)
- **Provider Portal**: [shaadisharthi.theworkpc.com/provider](https://shaadisharthi.theworkpc.com/provider)
- **Admin Panel**: [shaadisharthi.theworkpc.com/admin](https://shaadisharthi.theworkpc.com/admin)

---

## Project Overview

ShaadiSharthi enables:

- Customers to browse and book wedding services (search, filters, booking flow).
- Providers to register, manage services (CRUD), upload media (Cloudinary signed uploads), and manage bookings.
- Admins to manage users, approve providers, handle queries, and view analytics.

---

## � Individual Repositories

While this repository contains the complete Dockerized setup to run the entire stack, the source code for each application is also maintained in its own individual repository. This is useful for focused development on a single component.

- **Customer Frontend (Next.js)**: [github.com/nevil5651/shaadisharthi-customer](https://github.com/nevil5651/shaadisharthi-customer)
- **Provider Frontend (Angular)**: [github.com/nevil5651/shaadisharthi-provider](https://github.com/nevil5651/shaadisharthi-provider)
- **Admin Frontend (React)**: [github.com/nevil5651/shaadisharthi-admin](https://github.com/nevil5651/shaadisharthi-admin)
- **Backend (Java Servlets)**: [github.com/nevil5651/shaadisharthi-backend](https://github.com/nevil5651/shaadisharthi-backend)

---

## �🚀 Tech Stack

**Frontends**

- Customer: `Next.js` (TypeScript) + `Tailwind CSS`
- Provider: `Angular` (TypeScript) + `Bootstrap`
- Admin: `React` (JavaScript) + `Bootstrap`

**Backend**

- `Java` Servlets & JSP (Eclipse/Tomcat)
- `JDBC` + `MySQL` (HikariCP for Connection Pooling)
- Authentication: JWT (for Admin/Provider) & HttpOnly Cookies (for Customer).
- Real-time Notifications: WebSockets for customer notifications.

**DevOps / Deployment**

- Docker & Docker Compose
- Nginx as a reverse proxy

---

## ⚙️ Getting Started: Running the Project Locally

This project uses **MySQL** as its database. The following steps will guide you through setting it up automatically as a Docker container alongside the other application services. You do not need to install MySQL on your local machine.

This guide will walk you through setting up the entire ShaadiSharthi platform on your local machine using Docker.

### 1. Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Node.js and npm**: Required to build the frontend applications.
- **Angular CLI**: Required for the Angular provider portal. Install it globally:
  ```bash
  npm install -g @angular/cli
  ```
- **Docker and Docker Compose**: The core technologies used to run the application stack.

### 2. Clone the Repository

```bash
git clone https://github.com/nevil5651/shaadisharthi-webapp.git
cd shaadisharthi-webapp
```

### 3. Configure Environment Variables

You need to create four environment configuration files for the different parts of the application.

**A. Root `.env` file (for Docker Compose)**

Create a file named `.env` at the root of the project. This file provides credentials to the backend and database containers.

_File Location:_ `shaadisharthi-webapp/.env`

```env
# MySQL (used to start mysql container)
MYSQL_ROOT_PASSWORD=ROOT_PASSWORD
MYSQL_DATABASE=shaadisharthi
MYSQL_USER=appuser
MYSQL_PASSWORD=apppass

# App DB
DB_URL=jdbc:mysql://shaadi_mysql:3306/shaadisharthi
DB_USERNAME=appuser
DB_PASSWORD=apppass
DB_NAME=shaadisharthi
DB_DRIVER=com.mysql.cj.jdbc.Driver

# JWT
JWT_SECRET_KEY=create_a_long_key
JWT_RESET_SECRET_KEY=create_a_long_key

# Email
EMAIL_FROM=email
EMAIL_PASSWORD=app_password_of_16_alphabets_like_abcd_efgh_ijkl_mnop

# Base App Url
APP_BASE_URL=http://localhost

# Cloudinary
CLOUDINARY_CLOUD_NAME=cloud_name
CLOUDINARY_API_KEY=cloud_api_key
CLOUDINARY_API_SECRET=cloud_api_secret

# CORS / allowed origins
ADMIN_ALLOWED_ORIGINS=http://localhost
SERVICEPROVIDER_ALLOWED_ORIGINS=http://localhost
CUSTOMER_ALLOWED_ORIGINS=http://localhost


# Role mappings
ACCOUNT_GET_ROLES=super_admin,moderation_admin,support_admin,SERVICE_PROVIDER
ACCOUNT_POST_ROLES=moderation_admin,super_admin,support_admin,SERVICE_PROVIDER
DASHBOARDSTATS_GET_ROLES=super_admin,moderation_admin
CHANGEPASSWORD_POST_ROLES=super_admin
CUSTOMERS_GET_ROLES=moderation_admin,super_admin
CUSTOMERS_POST_ROLES=moderation_admin,super_admin
SERVICE_PROVIDERS_GET_ROLES=moderation_admin,super_admin
SERVICE_PROVIDERS_POST_ROLES=moderation_admin,super_admin
QUERIES_GET_ROLES=moderation_admin,super_admin
QUERIES_POST_ROLES=moderation_admin,super_admin
GUESTQUERYHANDLER_GET_ROLES=moderation_admin,super_admin
GUESTQUERYHANDLER_POST_ROLES=moderation_admin,super_admin
ADMINLOGOUT_POST_ROLES=moderation_admin,super_admin
UPDATE_STATUS_POST_ROLES=moderation_admin,super_admin
USERTRENDS_GET_ROLES=super_admin
ORDERCOMPARISON_GET_ROLES=super_admin
PROVIDERDASHBOARDSTATS_GET_ROLES=SERVICE_PROVIDER
LOGOUT_POST_ROLES=SERVICE_PROVIDER
PROVIDER_CHANGE_PASSWORD_POST_ROLES=SERVICE_PROVIDER
PROVIDERSERVICES_GET_ROLES=SERVICE_PROVIDER
CLOUDINARYSIGNATURE_POST_ROLES=SERVICE_PROVIDER
CREATESERVICE_POST_ROLES=SERVICE_PROVIDER
EDITSERVICE_PUT_ROLES=SERVICE_PROVIDER
EDITSERVICE_POST_ROLES=SERVICE_PROVIDER
EDITSERVICE_DELETE_ROLES=SERVICE_PROVIDER
DELETESERVICE_DELETE_ROLES=SERVICE_PROVIDER
BUSINESS_REGISTER_POST_ROLES=SERVICE_PROVIDER
ADDSUPPORTQUERY_POST_ROLES=SERVICE_PROVIDER
BOOKING_LIST_SERVLET_GET_ROLES=SERVICE_PROVIDER
BOOKING_ACTION_POST_ROLES=SERVICE_PROVIDER
SERVICES_GET_ROLES=CUSTOMER
REVIEWS_POST_ROLES=CUSTOMER
CSTMR_ACC_GET_ROLES=CUSTOMER
CSTMR_ACC_POST_ROLES=CUSTOMER
SERVICE_DETAIL_GET_ROLES=CUSTOMER
PROCESS_BOOKINGS_POST_ROLES=CUSTOMER
CSTMR_BOOKINGS_GET_ROLES=CUSTOMER
CSTMR_ACTION_POST_ROLES=CUSTOMER
CSTMR_UPCOMING_BOOKINGS_GET_ROLES=CUSTOMER
```

**B. Next.js Customer App `.env`**

Create a file named `.env` inside the `shaadisharthi-next` directory.

_File Location:_ `shaadisharthi-webapp/shaadisharthi-next/.env`

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_INTERNAL_API_URL=http://shaadisharthi-backend:8080
NEXT_PUBLIC_WEBSOCKET_API_URL=ws://localhost:8080/CustomerSocket
NODE_ENV=development
```

**C. React Admin App `.env`**

Create a file named `.env` inside the `shaadisharthi-react` directory.

_File Location:_ `shaadisharthi-webapp/shaadisharthi-react/.env`

```env
REACT_APP_API_URL=http://localhost:8080
```

**D. Angular Provider App `environment.ts`**

Create a file named `environment.ts` inside the `shaadisharthi-angular/src/environments/` directory.

_File Location:_ `shaadisharthi-webapp/shaadisharthi-angular/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: "http://localhost:8080",
  cloudinary: {
    cloudName: "your_cloudinary_cloud_name", // Must match the one in the root .env
  },
  supportEmail: "support@shaadisharthi.com",
};
```

### 4. Build and Run the Application

With the environment files configured, you can start the entire application stack with a single command from the root of the project.

```bash
docker compose up -d --build
```

This command will take a few minutes the first time as it downloads dependencies and builds the Docker images. Subsequent runs will be much faster.

> **Important Note on Startup Time:**
> After the containers are created, the frontend applications will be accessible almost immediately. However, the Java backend service takes approximately **60-90 seconds** to fully initialize _after_ the container creation message appears.
>
> During this time, you may see **"Network Error"** or **"404"** errors in the browser. This is expected. Please wait about 90 seconds, and the errors will resolve once the backend is ready.

---

### 5. Access the Applications

Your local ShaadiSharthi environment is now ready! You can access the different frontends at these URLs:

- **Customer Portal**: http://localhost/customer
- **Provider Portal**: http://localhost/provider
- **Admin Panel**: http://localhost/admin

To stop all the running services, use:

```bash
docker compose down
```

---

### 6. Accessing the Database Directly

If you need to inspect the MySQL database, run queries manually, or debug an issue, you can get a shell inside the running MySQL container

- 1. Open a new terminal window.
- 2. Run the following command from the root of the project:

```bash
docker compose exec mysql mysql -u ${MYSQL_USER} -p
```

- 3. You will be prompted to enter a password. Enter the value you set for MYSQL_PASSWORD in your root .env file.
- 4. You will now be inside the MySQL command-line client, connected to the `shaadisharthi` database.

---

### 7. Troubleshooting Backend Issues

If you encounter unexpected behavior or errors with the backend, you can inspect its logs. The backend uses **SLF4J with Logback** for logging, providing detailed output.

To view the logs for the `shaadisharthi-backend` container, open a new terminal and run:

```bash
docker compose logs shaadisharthi-backend
```

This command will display the real-time logs from the backend service, which can help diagnose issues.

---

### 8. Creating the First Admin User (Important First Step)

Before you can log in to the Admin Panel, you must **manually create the first admin user** directly in the database. The system is designed to force Two-Factor Authentication (2FA) setup on the first login, which requires the database record to be prepared correctly.

**Steps:**

1.  **Access the MySQL Database:**
    Follow the steps in the "Accessing the Database Directly" section above to get a SQL prompt inside the `shaadi_mysql` container.

2.  **Generate a BCrypt Hash for Your Password:**
    The application uses the **BCrypt** algorithm to securely hash and store all user passwords, ensuring they are never saved in plain text.
    For this manual step, you will need to generate a BCrypt hash for your desired password. You can use a free online BCrypt generator for this purpose for development u can keep cost factor 10.

3.  **Insert the Admin User:**
    Run the following SQL command. Replace the placeholder values with your own information. **Crucially, leave `totp_secret` as `NULL`**.

    ```sql
    INSERT INTO admin (name, email, phone_no, address, password, role, is_active, totp_secret)
    VALUES
    ('Your Full Name', 'your-admin-email@example.com', '1234567890', 'Your Address', 'your_generated_bcrypt_password_hash', 'super_admin', 1, NULL);
    ```

4.  **Log In:**
    You can now go to the Admin Panel and log in with the email and password you just set. You will be prompted to set up your 2FA (TOTP) with a QR code.

---

## Architecture & repository layout

```text
root/
├── docker-compose.yml            # Orchestrates all services.
├── deploy.sh                     # Optional build automation script.
├── nginx/
│   └── frontend.conf             # Nginx config: routes traffic to the correct application.
├── shaadisharthi-backend/
├── shaadisharthi-next/
├── shaadisharthi-angular/
└── shaadisharthi-react/
```

**High-level flow:** client → nginx (serves static frontends + proxies `/api/` to backend + proxies Next.js SSR + WebSocket `/ws/customer` to backend) → backend (Tomcat servlet) → MySQL / Cloudinary / Email service.

---

## Exact tech stack (what is currently implemented)

**Frontends**

- Customer: `Next.js` (TypeScript) + `Tailwind CSS` — dark/light system theme detection supported
- Provider: `Angular` (TypeScript) + Bootstrap
- Admin: `React` (JavaScript) + Bootstrap

**Backend**

- `Java` Servlets & JSP (Eclipse/Tomcat)
- `JDBC` + `MySQL` (HikariCP used for Connection Pooling)
- HTTP API endpoints (REST-like)
- JWT-based authentication is used for Admin roles and Provider via the Authorization header. HttpOnly cookies/session flows are used for Customer authentication.
- Cloudinary integration (signed uploads via backend)
- JavaMail for email sending (async via `ExecutorService`)
- Role-Based Access Control for admin subroles (handled by filters with jwt authentication)
- Rate limiting, CORS filtering
- We used SLF4J (Simple Logging Facade for Java) as the primary logging interface throughout the application.
- WebSockets are implemented for real-time notifications; if a Customer is currently connected, notifications are pushed directly to their active session.

**DevOps / Deployment**

- Docker & Docker Compose (multi-container stack)
- Nginx reverse proxy with routes for static frontends, Next.js SSR, and backend API proxying
- Automated `deploy.sh` script (local builds + docker-compose up) — used locally and on EC2 for deployments
- SSL via Let’s Encrypt (Certbot) configured in nginx container volume (certs stored in `certbot/`)
- Single EC2 node deployment (Elastic IP) — suitable as a live demo; not auto-scaled

---

## 📁 Important Files Explained

| File                                         | Purpose                                                                                                                                                   |
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `docker-compose.yml`                         | Defines MySQL, backend, Next.js, and nginx services for production.                                                                                       |
| `deploy.sh`                                  | Automates builds (Angular/React → static output), image builds, and compose run                                                                           |
| `nginx/frontend.conf`                        | Routes: `/api/` → backend, `/customer` → Next.js, `/admin` + `/provider` → static frontends, redirects HTTP→HTTPS , `/ws/customer` → WebSocket connection |
| `shaadisharthi-backend/docker/entrypoint.sh` | Generates `config.properties` dynamically and starts Tomcat after DB check                                                                                |
| `shaadisharthi-next/Dockerfile`              | Multi-stage build for Next.js SSR container                                                                                                               |

---

## ☁️ Cloudinary Upload Flow

The platform uses a secure and efficient flow for media uploads that avoids proxying files through the backend server:

1.  **Request Signature**: The provider's frontend asks the backend for a unique signature to authorize an upload.
2.  **Sign Payload**: The backend uses its `api_secret` to create a temporary, secure signature and sends it back to the client.
3.  **Direct Upload**: The client uploads the file directly to Cloudinary using the signature. This bypasses the backend, reducing server load and improving speed.
4.  **Store Metadata**: Cloudinary returns the file's `secure_url` and `public_id` to the client.
5.  **Persist Reference**: The client sends this URL and ID to the backend, which saves the reference in the database.

This architecture ensures scalable, low-latency uploads without handling file streams on the backend.

---

## 🔒 Security Notes

- **Deployment**: The current setup on a single EC2 instance is for demonstration purposes. A production environment would require load balancers, auto-scaling groups, and managed services (like Amazon RDS).
- **Secrets Management**: Sensitive data like JWT secrets and Cloudinary keys are passed via environment variables in `docker-compose`. For production, a dedicated service like AWS Secrets Manager is recommended.
- **Token Storage**: The React admin panel and Angular Provider currently uses `localStorage` for the JWT. For higher security, tokens should be stored in memory or, for web clients, in secure `HttpOnly` cookies, as implemented in the Next.js customer app.
- **Architecture**: The plan is to migrate the backend to Spring Boot, which will enable a more robust implementation of security patterns like the refresh token flow.

---

## 🚀 Roadmap

- **Backend Migration**: Refactor the existing Java Servlet application to **Spring Boot** with Spring Data JPA for a more modern, maintainable, and scalable architecture.
- **Authentication**: Implement a **refresh-token** flow to enhance security and user experience by allowing longer-lived sessions without compromising token safety.
- **CI/CD**: Set up a **GitHub Actions** pipeline to automate testing, Docker image builds, and deployments.
- **Infrastructure**: Evolve the deployment to a multi-node architecture with a managed database (e.g., AWS RDS) and an Application Load Balancer for HTTPS termination.
- **Real-Time Features**: Expand the WebSocket implementation to provide more real-time notifications for booking updates, user chats, and admin alerts.

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
