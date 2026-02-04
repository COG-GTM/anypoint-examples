# HTTP Multipart Request - Spring Boot

This Spring Boot microservice is a migration of the original Mulesoft http-multipart-request application. It handles file uploads via HTTP multipart requests.

## Features

- GET /uploadFile - Serves an HTML form for file upload
- POST /uploadFile - Receives uploaded file, extracts filename, logs content type, stores file to configurable directory, and returns file contents

## Prerequisites

- Java 17 or higher
- Gradle 8.x (or use the included Gradle wrapper)

## Building the Application

```bash
./gradlew build
```

## Running the Application

```bash
./gradlew bootRun
```

The application will start on port 8081 by default.

## Configuration

Properties can be configured in src/main/resources/application.properties or via environment variables:

- server.port (default: 8081) - HTTP server port
- file.storage.directory (default: /tmp) - Directory where uploaded files are stored
- spring.servlet.multipart.max-file-size (default: 10MB) - Maximum file size
- spring.servlet.multipart.max-request-size (default: 10MB) - Maximum request size

## Usage

1. Start the application
2. Open your browser and navigate to http://localhost:8081/uploadFile
3. Click Browse and choose a file to upload
4. Click Submit
5. The file will be saved to the configured storage directory and its contents will be displayed

## Running Tests

```bash
./gradlew test
```

## Migration Notes

This application was migrated from the original Mulesoft implementation located at ../http-multipart-request/. Key differences:

- Uses Spring Boot 3.2.x instead of Mule ESB
- Uses Gradle instead of Maven for build management
- Implements proper layered architecture (controller, service)
- Includes comprehensive error handling with @ControllerAdvice
- Includes unit tests with MockMvc
