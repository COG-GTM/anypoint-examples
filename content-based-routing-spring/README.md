# Content Based Routing - Spring Boot Migration

This is a Spring Boot migration of the MuleSoft [content-based-routing](../content-based-routing) example.

## Overview

This application demonstrates content-based routing using Spring Boot. It routes HTTP requests based on a `language` query parameter and returns greetings in different languages.

## Features

- HTTP endpoint at `/` accepting a `language` query parameter
- Routes to different responses based on language:
  - `Spanish` -> "Hola!"
  - `French` -> "Bonjour!"
  - Default (no language or unknown) -> "Hello!"
- Logging of all responses

## Requirements

- Java 17 or higher
- Maven 3.6+

## Running the Application

```bash
# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

The application will start on port 8081 by default.

## Testing

```bash
# Run all tests
mvn test
```

## Usage Examples

```bash
# Spanish greeting
curl "http://localhost:8081/?language=Spanish"
# Response: Hola!

# French greeting
curl "http://localhost:8081/?language=French"
# Response: Bonjour!

# Default (English) greeting
curl "http://localhost:8081/"
# Response: Hello!
```

## Migration Notes

This Spring Boot application replicates the functionality of the original MuleSoft example:

| MuleSoft Component | Spring Boot Equivalent |
|-------------------|------------------------|
| HTTP Listener | `@RestController` with `@GetMapping` |
| Choice Router | Java switch expression in `GreetingService` |
| Flow Variables | Method parameters and local variables |
| Sub-flow | Private method / Service method |
| Logger | SLF4J with `@Slf4j` |
| Expression Filter | Not needed (favicon handled by browser) |

## Project Structure

```
src/
├── main/
│   ├── java/org/mule/examples/contentbasedrouting/
│   │   ├── ContentBasedRoutingApplication.java  # Main application class
│   │   ├── GreetingController.java              # REST controller
│   │   └── GreetingService.java                 # Business logic
│   └── resources/
│       └── application.properties               # Configuration
└── test/
    └── java/org/mule/examples/contentbasedrouting/
        ├── GreetingControllerTest.java          # Integration tests
        └── GreetingServiceTest.java             # Unit tests
```
