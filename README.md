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

Database configuration can be modified in:
- `src/main/resources/application.properties` for local development
- `docker-compose.yml` for Docker deployment