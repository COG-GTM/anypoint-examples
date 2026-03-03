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

- Java 8 JDK installed (verify with `java -version`)
- Apache Maven 3.x installed (verify with `mvn -version`)

### Setup and Build

1. Clone the repository:
   ```bash
   git clone https://github.com/COG-GTM/anypoint-examples.git
   cd anypoint-examples
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Import into Anypoint Studio:
   - Open Anypoint Studio
   - File > Import > Maven-based Mule Project from pom.xml
   - Select the submodule you want to work with

### Java 8 Features Used

This codebase leverages the following Java 8 language features and APIs:

- **Diamond Operator**: Generic type inference with `<>` for cleaner collection instantiation
- **Lambda Expressions**: Concise anonymous function syntax replacing verbose anonymous inner classes
- **Streams API**: Declarative data processing pipelines using `stream()`, `filter()`, `map()`, and `collect()`
- **Method References**: Compact syntax like `Team::getPoints` for referencing existing methods
- **Comparator Composition**: Fluent comparator building with `Comparator.comparingInt()`, `thenComparing()`, and `reversed()`
- **forEach()**: Simplified iteration over collections using `Iterable.forEach()`
