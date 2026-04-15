# JUGBAQ Call for Papers

A modern Call for Papers management system for JUGBAQ (Java User Group Buenos Aires), built with Spring Boot and Vaadin.

## Features

- Multi-tenant event management
- Speaker registration and profile management
- Submission tracking with status workflow
- Review process with scoring and discussion
- Agenda planning and publishing
- iCal calendar export
- Email notifications
- OAuth2 authentication
- Rate limiting for API endpoints
- Test coverage >80%

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.13**
- **Spring Modulith 1.4.10** - Modular architecture
- **Vaadin 24.9.13** - UI framework
- **PostgreSQL** - Database
- **Testcontainers** - Integration testing
- **JaCoCo** - Code coverage
- **SonarQube** - Code quality analysis
- **Spotless** - Code formatting

## Prerequisites

- Java 21 or higher
- Maven 3.9+
- Docker and Docker Compose (for PostgreSQL and SonarQube)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd jugbaq-callforpapers
```

2. Start the infrastructure services:
```bash
docker-compose up -d
```

This starts:
- PostgreSQL database on port 5432
- SonarQube on port 9000

## Running the Application

Run the Spring Boot application:
```bash
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`

## Running Tests

Run all tests:
```bash
./mvnw test
```

Run specific test class:
```bash
./mvnw test -Dtest=ClassName
```

## Code Quality

### Spotless (Code Formatting)

Check formatting:
```bash
./mvnw spotless:check
```

Apply formatting:
```bash
./mvnw spotless:apply
```

### SonarQube Analysis

Run SonarQube analysis (requires SonarQube server running):
```bash
task sonar SONAR_TOKEN=<your-token>
```

Or using Maven directly:
```bash
./mvnw sonar:sonar -Dsonar.token=<your-token>
```

SonarQube dashboard: http://localhost:9000

## Project Structure

```
jugbaq-callforpapers/
├── src/main/java/com/jugbaq/cfp/
│   ├── events/          # Event management module
│   ├── submissions/     # Submission tracking module
│   ├── users/           # User management module
│   ├── review/          # Review process module
│   ├── publishing/      # Agenda publishing module
│   ├── notifications/   # Email notifications module
│   ├── security/        # OAuth2 configuration
│   └── shared/          # Shared utilities (tenant, logging, etc.)
├── src/test/java/       # Test classes
└── src/main/resources/  # Configuration files
```

## Coverage Exclusions

The following files are excluded from coverage analysis as they are difficult to unit-test:
- UI components (`**/ui/**/*.java`)
- Security configuration (`**/security/**/*.java`)
- Domain entities (`**/domain/*.java`)
- DTOs and summary records
- Event listeners
- Exception classes
- Controllers and filters

## Development Tasks

Use Task for common development tasks:

```bash
task              # List all available tasks
task test         # Run tests
task sonar        # Run SonarQube analysis
task infra-up     # Start infrastructure services
task infra-down   # Stop infrastructure services
```

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Run tests: `./mvnw test`
4. Apply code formatting: `./mvnw spotless:apply`
5. Run SonarQube analysis to ensure quality
6. Submit a pull request

## License

[Add your license here]

## Contact

JUGBAQ - Java User Group Barranquilla
