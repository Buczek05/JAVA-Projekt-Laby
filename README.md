# Budget Application

A Spring Boot application for budget management with PostgreSQL database.

## Docker Configuration

This project includes Docker configuration for easy setup and deployment.

### Prerequisites

- Docker
- Docker Compose

### Running with Docker

1. Build and start the application with PostgreSQL:

```bash
docker-compose up -d
```

2. The application will be available at http://localhost:8080

3. To stop the application:

```bash
docker-compose down
```

4. To stop the application and remove volumes (this will delete the database data):

```bash
docker-compose down -v
```

### Database Information

- PostgreSQL database is exposed on port 5432
- Database name: budget
- Username: postgres
- Password: postgres

## Development

### Running Locally

To run the application locally without Docker:

1. Make sure you have PostgreSQL installed and running on port 5432
2. Create a database named 'budget'
3. Run the application using Maven:

```bash
./mvnw spring-boot:run
```

### Configuration

The application uses Spring profiles for environment-specific configurations:

- **dev** (default): Development environment configuration
  - Uses local PostgreSQL database
  - Shows detailed SQL logs
  - Automatically updates database schema

- **prod**: Production environment configuration
  - Used when running with Docker
  - Optimized connection pool settings
  - Minimal logging
  - Validates database schema instead of updating it

- **test**: Testing environment configuration
  - Uses a separate test database
  - Creates and drops database schema for each test run
  - Shows detailed SQL logs for debugging

#### How to specify a profile:

1. Using command line:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Using environment variable:
```bash
export SPRING_PROFILES_ACTIVE=prod
./mvnw spring-boot:run
```

3. In Docker (already configured in docker-compose.yml):
```bash
docker-compose up -d
```

Configuration files:
- `src/main/resources/application.properties`: Common settings
- `src/main/resources/application-dev.properties`: Development settings
- `src/main/resources/application-prod.properties`: Production settings
- `src/main/resources/application-test.properties`: Testing settings
