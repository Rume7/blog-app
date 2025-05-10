# 📝 Blog App Backend (Spring Boot + Postgres + Redis)

Welcome to the **Blog App Backend API** — a Spring Boot-based REST API built for managing blog posts, user
subscriptions, and more.  
This backend is fully containerized and runs via **Docker Compose** (Postgres + Redis + App).

---

## 🚀 Tech Stack

- **Java 17** + **Spring Boot**
- **Postgres** (Database)
- **Redis** (Caching layer)
- **Docker Compose** (Environment orchestration)
- **Swagger/OpenAPI** (API Docs)
- **JWT Auth** (Secured endpoints)
- **Rate Limiting** (via filter)

---

## 📦 Features

- Blog Posts CRUD (with search and filters)
- Subscriber management (subscribe, unsubscribe, resubscribe)
- JWT Authentication
- Redis Caching
- Swagger API Documentation
- Rate Limiting protection

---

## ⚠️ Note: Runs on Docker only

This app is designed to be run **via Docker Compose**.  
❌ It does **not** support running directly on local machine (without Docker).

---

## 🐳 Prerequisites

Make sure you have the following installed on your system:

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## 🔥 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/rume7/blog-app.git
cd blog-app

```

### 2. Configure Environment Variables

```

Edit the .env file at the root directory. Example:
DB_USERNAME=bloguser
DB_PASSWORD=blogpass
DB_NAME=blogdb
JWT_SECRET=your_secret_key

```

### 3. Run the App using Docker Compose

```
docker-compose up --build

```

## 🌐 API Documentation

### Once the app is running, open the Swagger UI in your browser:

````
http://localhost:8080/swagger-ui.html
