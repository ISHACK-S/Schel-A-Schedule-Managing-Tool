# Schel

Schel is a web-enabled schedule management system built using Java 21, Supabase PostgreSQL, REST APIs, and a responsive web frontend.

> **Important:** This repository does not provide a ready-to-use database or production credentials. Anyone setting up Schel must create their **own Supabase project, database tables, and environment variables**.

## Features

- User Registration and Login
- BCrypt Password Hashing
- Schedule CRUD Operations
- Category Management
- Reminder Management
- Dashboard
- Input Validation
- User-specific Data Management
- Supabase PostgreSQL Integration
- Supabase REST API Integration
- Responsive Web Frontend
- Layered Java Architecture
- Exception Handling

## Tech Stack

### Backend
- Java 21
- Maven
- Java HttpClient
- Jackson
- BCrypt

### Frontend
- HTML5
- CSS3
- JavaScript

### Database
- Supabase
- PostgreSQL
- Supabase REST API

### Tools
- IntelliJ IDEA
- Git
- GitHub

## Architecture

```text
                    WEB FRONTEND
                         |
                         v
                    CONTROLLER
                         |
                         v
                      SERVICE
                         |
                         v
                    REPOSITORY
                         |
                         v
                SUPABASE REST API
                         |
                         v
                SUPABASE POSTGRESQL
```

## Project Structure

```text
Schel/
├── src/main/java/com/schel/
│   ├── config/
│   ├── controllers/
│   ├── database/
│   ├── exceptions/
│   ├── menus/
│   ├── models/
│   ├── repository/
│   ├── services/
│   └── utils/
├── frontend/
├── pom.xml
├── .gitignore
└── README.md
```

# Setup

## 1. Clone the Repository

```bash
git clone https://github.com/ISHACK-S/Schel-A-Schedule-Managing-Tool.git
cd Schel-A-Schedule-Managing-Tool
```

## 2. Create Your Own Supabase Project

Schel requires PostgreSQL through Supabase.

Create your own project at:

https://supabase.com/

You will need your own:

- Supabase Project URL
- Supabase API Key

**Do not use someone else's credentials. Do not commit your API keys to GitHub.**

## 3. Create Your Own Database Tables

The repository does **not** provide a shared or pre-configured database.

Create these tables in your own Supabase project:

- `users`
- `categories`
- `schedules`
- `reminders`

### Users

```sql
create table users (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    email text unique not null,
    password text not null,
    created_at timestamptz default now()
);
```

### Categories

```sql
create table categories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    name text not null,
    color text,
    created_at timestamptz default now()
);
```

### Schedules

```sql
create table schedules (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    category_id uuid,
    title text not null,
    description text,
    task_date date not null,
    start_time time not null,
    end_time time not null,
    priority text not null,
    status text not null,
    created_at timestamptz default now(),
    updated_at timestamptz default now()
);
```

Allowed priority values:

```text
LOW
MEDIUM
HIGH
```

Allowed status values:

```text
PENDING
COMPLETED
MISSED
```

Recommended constraints:

```sql
alter table schedules
add constraint schedules_priority_check
check (priority in ('LOW', 'MEDIUM', 'HIGH'));

alter table schedules
add constraint schedules_status_check
check (status in ('PENDING', 'COMPLETED', 'MISSED'));
```

### Reminders

```sql
create table reminders (
    id uuid primary key default gen_random_uuid(),
    schedule_id uuid not null,
    reminder_time timestamptz not null,
    is_sent boolean default false
);
```

> **Note:** Keep the database schema compatible with the Java models and repository code. If you modify the models, update your database schema accordingly.

## 4. Configure Environment Variables

Use **your own** Supabase credentials.

```env
SUPABASE_URL=your_supabase_project_url
SUPABASE_API_KEY=your_supabase_api_key
```

Example:

```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_API_KEY=your-api-key
```

Do not use the example values as real credentials.

### Keep secrets out of Git

Your `.env` file should never be committed.

Recommended `.gitignore` entries:

```gitignore
.env
.env.*
!.env.example
```

You can commit an `.env.example` containing only:

```env
SUPABASE_URL=
SUPABASE_API_KEY=
```

## 5. Configure the Application

Make sure the application reads:

```text
SUPABASE_URL
SUPABASE_API_KEY
```

If your local version uses `DatabaseConfig.java`, configure it to read environment variables rather than storing credentials directly in source code.

## 6. Install Requirements

You need:

- Java 21 or later
- Maven
- Git
- A Supabase account

Check Java:

```bash
java -version
```

Check Maven:

```bash
mvn -version
```

## 7. Build

```bash
mvn clean install
```

## 8. Run

Run `Main.java` from IntelliJ IDEA, or use the Maven command configured by your project.

## 9. Frontend

Configure the frontend to use your local backend/API endpoint. Do not point it at another developer's server.

# Database Ownership

Every installation should use its own Supabase project:

```text
Developer A
    |
    +-- Supabase Project A
           +-- users
           +-- categories
           +-- schedules
           +-- reminders

Developer B
    |
    +-- Supabase Project B
           +-- users
           +-- categories
           +-- schedules
           +-- reminders
```

In short:

**Clone → Create your own Supabase project → Create your own tables → Add your own environment variables → Build → Run**

# Security Warning

Never commit secrets such as:

```text
SUPABASE_API_KEY
SUPABASE_SERVICE_ROLE_KEY
DATABASE_PASSWORD
JWT_SECRET
```

to GitHub.

If a secret is accidentally exposed, revoke/rotate it immediately.

# Troubleshooting

### Database connection fails

Check:

- Your Supabase project is active
- `SUPABASE_URL` is correct
- `SUPABASE_API_KEY` is correct
- Required tables exist
- Table names and columns match the application

### Registration fails

Check that `users` contains:

```text
id
name
email
password
created_at
```

Also make sure the email is not already registered.

### Schedule creation fails

Check:

- `task_date`
- `start_time`
- `end_time`
- `priority`
- `status`

Priority must be `LOW`, `MEDIUM`, or `HIGH`.

Status must be `PENDING`, `COMPLETED`, or `MISSED`.

The end time must be after the start time.

# Future Enhancements

- Intelligent schedule conflict detection
- Alternative time-slot suggestions
- Priority-aware task ordering
- External calendar synchronization
- Email and browser notifications
- Mobile notifications
- Machine-learning-based scheduling recommendations
- Native mobile application
- Collaborative scheduling

Machine-learning functionality is part of the future roadmap and is not required for the basic Schel setup.

# Repository

https://github.com/ISHACK-S/Schel-A-Schedule-Managing-Tool

# Authors

### Ishack S
Computer Science and Engineering  
Chennai Institute of Technology

### Mohammed Aariz Dhayan R
Computer Science and Engineering  
Chennai Institute of Technology

# License

This project was developed as an academic Project-Based Learning project.

If you intend to reuse, modify, or redistribute the project, please check with the repository owner regarding the applicable usage terms.
