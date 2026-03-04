Anypoint Examples
====================

This repository is a result of the Anypoint Examples initiative.
It contains example Mule applications that illustrate particular concepts within the Anypoint platform. 

You may download the example projects and import them to Anypoint Studio.

These examples are also available from Mulesoft Exchange and can be imported to Anypoint Studio directly.

You may also access the README.md file from the folder for each of these examples.
The README.md file contains the use case and instructions on how to setup and run these examples.

## Java Version

This project requires **Java 8 (JDK 1.8)** or later.

### Prerequisites

- Java Development Kit (JDK) 8 or later installed
- Apache Maven 3.x installed
- Anypoint Studio (optional, for IDE-based development)

### Setup and Build

1. Ensure your `JAVA_HOME` environment variable points to a JDK 8 installation:
   ```bash
   export JAVA_HOME=/path/to/jdk8
   ```

2. Clone the repository:
   ```bash
   git clone https://github.com/COG-GTM/anypoint-examples.git
   cd anypoint-examples
   ```

3. Build the project using Maven:
   ```bash
   mvn clean install
   ```

4. To build a specific module:
   ```bash
   mvn clean install -pl <module-name>
   ```

### Java 8 Features

This project uses the following Java 8 features:
- **Diamond operator** (`<>`) for cleaner generic type inference
- **Lambda expressions** for concise functional-style programming
- **Stream API** for declarative collection processing
- **Method references** for improved readability

The `maven-compiler-plugin` is configured in the parent `pom.xml` with source and target set to `1.8`.
