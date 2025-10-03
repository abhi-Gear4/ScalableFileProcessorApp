Scalablefileprocessor Application
Introduction
Scalablefileprocessor is a Spring Boot application designed to process large JSON Lines (.jsonl) files containing user data efficiently. The application reads files from a specified directory, validates user records, saves valid users in the database, and logs malformed or invalid records as bad data. It supports batch processing, asynchronous execution, and throttling to handle high volumes of data.

# Build and Run Instructions for Local Env

Prerequisites

1) Java JDK21

2) Docker installed and running (required for PostgreSQL Testcontainers during testing)

3) Gradle build management

Building the Project
Use Gradle wrapper to build:

bash
./gradlew clean build

# Build Notes: JDK Compatibility
The project is built using JDK 21 (leveraging Java 21 features and APIs).

It cannot be built on JDK 24 or higher at this time due to Gradle/toolchain limitations and compatibility issues.

The application can run on JDK 21, 22, 23, and 24 runtime environments, but build toolchain must be JDK 21.

# Configuration Properties (Performance and Tuning)

| Property                      | Description                                         | Recommended Value                                        |
|-------------------------------|-----------------------------------------------------|----------------------------------------------------------|
| fileprocessor.batchSize        | Number of user records batched before saving to the database. | 10 for tests; 50-100+ for production workloads           |
| fileprocessor.maxThreads       | Maximum threads for concurrent file processing.    | Match your CPU cores; 4 is a good baseline               |
| fileprocessor.maxFilesPerMinute| Maximum file processing rate to prevent system overload. | 40 for our test sample, increase it for fast test results|


# Running Unit and Integration Tests

Integration tests use Testcontainers to spin up a PostgreSQL container. Run tests with:

bash
./gradlew clean test


## Building the Project in a Repeatable Docker Environment

To ensure fully repeatable builds — without relying on the host machine's Java or Gradle setup — we use Docker to run the entire build process. This includes compiling, testing, running Testcontainers-based integration tests, and building the final JAR.

### Build and Run Manually Inside the Custom Docker Image (`zulu-jdk24`)


<pre><code>
Step 1: Build the Docker Image from Dockerfile-zulu-jdk24

docker build -t zulu-jdk24 -f Dockerfile-zulu-jdk24 .
</code></pre>

<pre><code>
Step 2: Run an interactive shell in the container

docker run -it --privileged \
  --name zulu-container \
  -v /var/run/docker.sock:/var/run/docker.sock:rw \
  -v "$(pwd)":/app \
  -w /app \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  zulu-jdk24 \
  bash
</code></pre>

<pre><code>
Step 3: Inside the container, run the build manually

./gradlew clean build
</code></pre>

you can exit the container by typing:

<pre><code>
you can exit the container by typing:

exit
</code></pre>

Docker-in-Docker support: Mounting the Docker socket (/var/run/docker.sock) with --privileged allows Testcontainers (used in integration tests) to spin up temporary Docker containers during testing.

Environment variables:

TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal ensures Testcontainers can properly connect to Docker on macOS/Windows hosts.

TESTCONTAINERS_RYUK_DISABLED=true disables the Ryuk container cleaner when it’s not needed or causes permission issues in some Docker environments.

The packaged application JAR will be located at:
app/build/libs/scalablefileprocessor-0.0.1-SNAPSHOT.jar

After the build completes, the test reports are generated locally at:
app/build/reports/tests/test/index.html

* Viewing the Test Report

You can open the test report in your default browser using the following commands, because the project directory is mounted between the container and your host machine, so all build outputs (including test reports) are available on your host filesystem:
* macOS terminal:  
open build/reports/tests/test/index.html

*  Windows (PowerShell or CMD):     
* start build\reports\tests\test\index.html

# Assumptions:

The input data format is strictly JSON Lines (.jsonl) with one JSON object per line.

Mandatory fields for valid user records are last_name, date_of_birth, and postal_code in the address.

Records with missing mandatory fields are treated as bad data and saved in bad_data table.

Date fields are assumed valid if parsable; invalid date strings are accepted unless additional validation is implemented.

The batch size, threading, and throttling properties are configurable via application properties.

The application is primarily designed for batch processing of files placed in a designated directory.

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