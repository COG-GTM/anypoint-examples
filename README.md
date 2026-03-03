Anypoint Examples
====================

This repository is a result of the Anypoint Examples initiative.
It contains example Mule applications that illustrate particular concepts within the Anypoint platform. 

You may download the example projects and import them to Anypoint Studio.

These examples are also available from Mulesoft Exchange and can be imported to Anypoint Studio directly.

You may also access the README.md file from the folder for each of these examples.
The README.md file contains the use case and instructions on how to setup and run these examples.

## Prerequisites

- **Java 8 (JDK 1.8)** or higher is required to build and run these examples.
- **Apache Maven 3.x** is required as the build tool.
- **Anypoint Studio 6.x** or later (recommended for importing and running examples).

## Setup and Build Instructions

1. **Install Java 8 JDK**
   - Download and install Java 8 JDK from [Oracle](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) or [AdoptOpenJDK](https://adoptopenjdk.net/).
   - Set the `JAVA_HOME` environment variable to point to your JDK 8 installation.
   - Verify by running: `java -version` (should show version 1.8.x).

2. **Install Apache Maven**
   - Download and install Maven from [https://maven.apache.org/](https://maven.apache.org/).
   - Verify by running: `mvn -version`.

3. **Clone the Repository**
   ```bash
   git clone https://github.com/COG-GTM/anypoint-examples.git
   cd anypoint-examples
   ```

4. **Build All Examples**
   ```bash
   mvn clean install -DskipTests
   ```

5. **Run Tests**
   ```bash
   mvn test
   ```

6. **Import into Anypoint Studio**
   - Open Anypoint Studio.
   - Go to `File > Import > Anypoint Studio > Anypoint Studio project from File System`.
   - Select the example folder you want to import.

## Java 8 Features

This project has been upgraded to Java 8 and leverages the following language features and APIs:

- **Lambda Expressions**: Used to replace anonymous inner classes for functional interfaces (e.g., `Comparator`).
- **Streams API**: Applied for concise and expressive data processing, including filtering, mapping, and collecting operations.
- **Method References**: Used where applicable to improve code readability (e.g., `Team::getPoints`).
- **Diamond Operator (`<>`)**: Applied throughout the codebase for cleaner generic type inference.
- **`Comparator` Enhancements**: Leveraging `Comparator.comparingInt()`, `.reversed()`, and `.thenComparing()` for multi-criteria sorting.

## Project Structure

This is a multi-module Maven project. The root `pom.xml` defines shared dependencies and plugins, while each subdirectory contains an individual Mule application example with its own `pom.xml`.

The project is configured to compile with Java 8 source and target compatibility via the `maven-compiler-plugin` in the root POM.
