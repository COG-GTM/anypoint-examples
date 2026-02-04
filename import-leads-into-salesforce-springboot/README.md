# Import Leads into Salesforce - Spring Boot Microservice

A Spring Boot microservice that processes CSV files containing lead data and imports them into Salesforce. This is a migration from the original Mulesoft implementation.

## Features

- Polls a configurable input directory for CSV files (default: every 10 seconds)
- Parses CSV files with lead data (Company, Email, FirstName, LastName)
- Checks for existing leads in Salesforce by email to avoid duplicates
- Creates new leads in Salesforce in batches of 200
- Moves processed files to an output directory
- Logs success/failure/skipped record counts
- REST API endpoints for manual file upload and status checking

## Prerequisites

- Java 17 or higher
- Gradle 8.x
- Salesforce account with API access
- Salesforce security token (if required by your org)

## Project Structure

```
import-leads-into-salesforce-springboot/
├── build.gradle
├── settings.gradle
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/importleads/
    │   │   ├── ImportLeadsApplication.java
    │   │   ├── config/
    │   │   │   ├── FilePollingConfig.java
    │   │   │   └── SalesforceConfig.java
    │   │   ├── controller/
    │   │   │   └── LeadImportController.java
    │   │   ├── exception/
    │   │   │   ├── CsvProcessingException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── SalesforceException.java
    │   │   ├── model/
    │   │   │   ├── Lead.java
    │   │   │   └── ProcessingResult.java
    │   │   └── service/
    │   │       ├── CsvService.java
    │   │       ├── FilePollingService.java
    │   │       └── SalesforceService.java
    │   └── resources/
    │       ├── application.properties
    │       └── application-dev.properties
    └── test/
        └── java/com/example/importleads/
            ├── ImportLeadsApplicationTests.java
            ├── config/
            ├── model/
            └── service/
```

## Configuration

Configure the application using environment variables or `application.properties`:

### Salesforce Configuration

| Property | Environment Variable | Description | Default |
|----------|---------------------|-------------|---------|
| `salesforce.username` | `SALESFORCE_USERNAME` | Salesforce username | `alex@cognition.sandbox` |
| `salesforce.password` | `SALESFORCE_PASSWORD` | Salesforce password | - |
| `salesforce.security-token` | `SALESFORCE_SECURITY_TOKEN` | Salesforce security token | - |
| `salesforce.login-url` | `SALESFORCE_LOGIN_URL` | Salesforce login URL | `https://test.salesforce.com` |

For production Salesforce orgs, use `https://login.salesforce.com` as the login URL.
For sandbox orgs, use `https://test.salesforce.com`.

### File Polling Configuration

| Property | Environment Variable | Description | Default |
|----------|---------------------|-------------|---------|
| `file.polling.input-directory` | `INPUT_DIRECTORY` | Directory to poll for CSV files | `input` |
| `file.polling.output-directory` | `OUTPUT_DIRECTORY` | Directory for processed files | `output` |
| `file.polling.interval-ms` | `POLLING_INTERVAL_MS` | Polling interval in milliseconds | `10000` |

## Building the Application

```bash
# Navigate to the project directory
cd import-leads-into-salesforce-springboot

# Build the application
./gradlew build

# Build without running tests
./gradlew build -x test
```

## Running the Application

### Using Gradle

```bash
# Set environment variables
export SALESFORCE_USERNAME=your-username@example.com
export SALESFORCE_PASSWORD=your-password
export SALESFORCE_SECURITY_TOKEN=your-security-token

# Run the application
./gradlew bootRun
```

### Using JAR

```bash
# Build the JAR
./gradlew bootJar

# Run the JAR
java -jar build/libs/import-leads-into-salesforce-springboot-1.0.0.jar \
  --salesforce.username=your-username@example.com \
  --salesforce.password=your-password \
  --salesforce.security-token=your-security-token
```

### Using Docker (optional)

```bash
# Build Docker image
docker build -t import-leads-springboot .

# Run container
docker run -p 8080:8080 \
  -e SALESFORCE_USERNAME=your-username@example.com \
  -e SALESFORCE_PASSWORD=your-password \
  -e SALESFORCE_SECURITY_TOKEN=your-security-token \
  -v /path/to/input:/app/input \
  -v /path/to/output:/app/output \
  import-leads-springboot
```

## CSV File Format

The CSV file should have the following columns:

```csv
Company,FirstName,LastName,Birthday,Email
Acme Corp,John,Doe,01/15/1990,john.doe@acme.com
Tech Inc,Jane,Smith,02/20/1985,jane.smith@tech.com
```

Required fields for Salesforce Lead creation:
- `Company` - Company name
- `LastName` - Last name
- `Email` - Email address (used for duplicate checking)

Optional fields:
- `FirstName` - First name
- `Birthday` - Birthday (not sent to Salesforce in current implementation)

## REST API Endpoints

### Check Status

```bash
GET /api/leads/status
```

Returns the current status including Salesforce connection state and configured directories.

### Upload CSV File

```bash
POST /api/leads/upload
Content-Type: multipart/form-data

file: <CSV file>
```

Uploads and immediately processes a CSV file.

### Trigger Polling

```bash
POST /api/leads/trigger
```

Manually triggers the file polling process.

## Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with verbose output
./gradlew test --info
```

## Logging

The application uses SLF4J with Logback for logging. Log levels can be configured in `application.properties`:

```properties
# Set log level for the application
logging.level.com.example.importleads=DEBUG

# Set root log level
logging.level.root=INFO
```

## Migration from Mulesoft

This Spring Boot application replaces the original Mulesoft implementation with the following mappings:

| Mulesoft Component | Spring Boot Equivalent |
|-------------------|------------------------|
| File Inbound Endpoint | `@Scheduled` polling in `FilePollingService` |
| DataWeave Transform | `CsvService` with OpenCSV |
| Salesforce Connector | `SalesforceService` with Force.com WSC |
| Batch Processing | Batch creation in `SalesforceService.createLeads()` |
| Logger | SLF4J logging throughout |

## Troubleshooting

### Connection Issues

If you encounter Salesforce connection issues:

1. Verify your credentials are correct
2. Check if your IP is whitelisted in Salesforce (Setup > Security > Network Access)
3. Ensure you're using the correct login URL (test.salesforce.com for sandbox, login.salesforce.com for production)
4. Verify your security token is current (reset it in Salesforce if needed)

### File Processing Issues

1. Ensure the input directory exists and is writable
2. Check that CSV files have the correct column headers
3. Review application logs for detailed error messages

## License

This project is licensed under the same terms as the original Mulesoft example.
