# Hello World Spring Boot

This is a Spring Boot microservice that provides a simple HTTP request-response endpoint. It is a migration of the original Mulesoft hello-world application to Spring Boot.

## Overview

The application exposes a REST endpoint that returns a "Hello World" message when accessed.

## Prerequisites

- Java 17 or higher
- Gradle 7.x or higher (or use the included Gradle wrapper)

## Building the Application

To build the application, run:

```bash
./gradlew build
```

This will compile the code, run the tests, and create an executable JAR file.

## Running the Application

### Using Gradle

```bash
./gradlew bootRun
```

### Using the JAR file

After building, you can run the application using:

```bash
java -jar build/libs/hello-world-springboot-1.0.0.jar
```

## Configuration

The application runs on port 8081 by default. You can change this by modifying `src/main/resources/application.properties` or by setting an environment variable:

```bash
java -jar build/libs/hello-world-springboot-1.0.0.jar --server.port=9090
```

## API Endpoint

### GET /helloWorld

Returns a "Hello World" message.

**Request:**
```
GET http://localhost:8081/helloWorld
```

**Response:**
```
Hello World
```

## Running Tests

To run the unit tests:

```bash
./gradlew test
```

## Project Structure

```
hello-world-springboot/
├── build.gradle
├── settings.gradle
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/helloworld/
    │   │       ├── HelloWorldApplication.java
    │   │       ├── controller/
    │   │       │   └── HelloWorldController.java
    │   │       └── service/
    │   │           └── HelloWorldService.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/
            └── com/example/helloworld/
                ├── HelloWorldApplicationTests.java
                ├── controller/
                │   └── HelloWorldControllerTest.java
                └── service/
                    └── HelloWorldServiceTest.java
```

## Migration from Mulesoft

This Spring Boot application replaces the original Mulesoft hello-world application. The functionality is identical:

- **Original Mulesoft**: HTTP listener on port 8081 at path `/helloWorld` returning "Hello World"
- **Spring Boot**: REST controller with GET endpoint at `/helloWorld` returning "Hello World"

The Spring Boot version provides the same functionality with a modern, lightweight framework that is easier to maintain and deploy.
