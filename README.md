Anypoint Examples
====================

This repository is a result of the Anypoint Examples initiative.
It contains example Mule applications that illustrate particular concepts within the Anypoint platform. 

## Prerequisites

- **Java 8** (JDK 1.8) or higher is required to build and run these examples.
- **Apache Maven 3.x** for building the project.
- **Anypoint Studio** (optional) for importing and running examples in the IDE.

## Setup and Running

1. **Install Java 8 JDK**: Download and install JDK 1.8 from [Oracle](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) or [AdoptOpenJDK](https://adoptopenjdk.net/).
2. **Verify Java version**: Run `java -version` to confirm Java 8 is installed.
3. **Install Maven**: Download from [Maven](https://maven.apache.org/download.cgi) and follow installation instructions.
4. **Clone the repository**:
   ```bash
   git clone https://github.com/COG-GTM/anypoint-examples.git
   cd anypoint-examples
   ```
5. **Build the project**:
   ```bash
   mvn clean compile
   ```
6. **Run tests** (optional):
   ```bash
   mvn test
   ```

## Java 8 Features

This project has been upgraded from Java 7 to Java 8. The following Java 8 features are used throughout the codebase:

- **Lambda expressions**: Used to replace anonymous inner classes (e.g., `Comparator` implementations).
- **Streams API**: Used for filtering, mapping, and collecting data in a more declarative style.
- **Method references**: Used where applicable to improve code readability.
- **Diamond operator (`<>`)**: Used to simplify generic type declarations.
- **`java.time` API**: Used for date/time operations via `LocalDateTime`, `ZoneId`, and `DateTimeFormatter`, replacing legacy `Calendar`/`GregorianCalendar`/`SimpleDateFormat` usage where feasible.
- **`Optional`**: Used for null-safe value retrieval.
- **`forEach`**: Used for iterable processing with lambda callbacks.

## Importing to Anypoint Studio

You may download the example projects and import them to Anypoint Studio.

These examples are also available from Mulesoft Exchange and can be imported to Anypoint Studio directly.

You may also access the README.md file from the folder for each of these examples.
The README.md file contains the use case and instructions on how to setup and run these examples.
