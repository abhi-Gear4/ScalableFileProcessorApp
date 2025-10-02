Scalablefileprocessor Application
Introduction
Scalablefileprocessor is a Spring Boot application designed to process large JSON Lines (.jsonl) files containing user data efficiently. The application reads files from a specified directory, validates user records, saves valid users in the database, and logs malformed or invalid records as bad data. It supports batch processing, asynchronous execution, and throttling to handle high volumes of data.

# Build and Run Instructions

Prerequisites

1) Java 21 or higher installed

2) Docker installed and running (required for PostgreSQL Testcontainers during testing)

3) Gradle build management

Building the Project
Use Gradle wrapper to build:

bash
./gradlew clean build

Running the Application
Run the Spring Boot application:

bash 
./gradlew bootRun

Configuration Properties

* Number of user records to batch per database save operation
  fileprocessor.batchSize=100

* Maximum number of threads used for file processing concurrency
  fileprocessor.maxThreads=4

* Maximum number of files processed per minute to control throughput
  fileprocessor.maxFilesPerMinute=10

# Running Integration Tests

Integration tests use Testcontainers to spin up a PostgreSQL container. Run tests with:

bash
./gradlew clean test


# Assumptions:

The input data format is strictly JSON Lines (.jsonl) with one JSON object per line.

Mandatory fields for valid user records are last_name, date_of_birth, and postal_code in the address.

Records with missing mandatory fields are treated as bad data and saved in bad_data table.

Date fields are assumed valid if parsable; invalid date strings are accepted unless additional validation is implemented.

The batch size, threading, and throttling properties are configurable via application properties.

The application is primarily designed for batch processing of files placed in a designated directory.
"src/main/resources/jsonldata-directory"

# Observability and Application Performance Monitoring (APM)
For production readiness, the following observability features are recommended:

Metrics Collection: Use Micrometer to instrument key metrics such as number of files processed, users saved, bad data entries, processing duration per file, and thread pool usage.

Distributed Tracing: Integrate tracing (e.g., OpenTelemetry) to correlate requests and file processing workflows across microservices or system components.

Logging: Implement structured logging with trace and span IDs for easier troubleshooting and aggregation in log management tools like flentbit.

Health Checks: Provide both liveness and readiness checks to Kubernetes or orchestration platforms.

Alerting: Set up alerts on error rates, processing delays, or resource saturation thresholds.

Dashboarding: Visualize application throughput, latency, error quotas, and resource utilization on APM dashboards (e.g., Dynatrace, New Relic, Grafana).

# Resiliency Considerations
To ensure resilient and robust operation in production:

Retry and Backoff: Implement retries with exponential backoff for transient database or file system errors.

Circuit Breaker: Use circuit breaker patterns to prevent cascading failures when downstream components are degraded.

Throttling and Rate Limiting: Control file processing rate to avoid system overload using configurable limits.

Bulkhead Isolation: Separate processing resources (e.g., thread pools) for different tasks or file sets to contain failures.

Data Consistency: Use transactional boundaries during database writes with rollback on failure to ensure data integrity.

Validation: Strict validation of input format and data before processing to minimize error propagation.