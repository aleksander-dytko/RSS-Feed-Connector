# Copilot Instructions for RSS Feed Connector

## Project Overview

This is a **Camunda 8 Outbound Connector** that fetches and parses RSS/Atom feeds using the ROME library. The connector integrates RSS feeds into BPMN workflows with filtering capabilities (date ranges, item limits).

## Architecture & Key Components

### Core Structure

- **Main Connector**: `RssFeedConnectorFunction` - implements `OutboundConnectorFunction` with `@OutboundConnector` annotation
- **DTOs**: Located in `dto/` package using Java records for immutability
  - `RssFeedRequest` - input validation and template property annotations
  - `RssFeedResult` - structured output with items, metadata, and counts
  - `RssFeedItem` - individual feed item with standard RSS fields
  - `FeedMetadata` - feed-level information (title, description, lastBuildDate)

### Service Registration Pattern

Connectors are auto-discovered via `META-INF/services/io.camunda.connector.api.outbound.OutboundConnectorFunction` containing the fully qualified class name.

### Element Template Generation

Element templates are **auto-generated** during Maven package phase using:

- `@ElementTemplate` annotation on connector class
- `@TemplateProperty` annotations on request record fields
- Output: `element-templates/rss-feed-connector.json`

## Development Workflows

### Build & Test Commands

```bash
# Quick development workflow
./run-connector.sh test          # Run unit tests
./run-connector.sh local         # Connect to local Camunda
./run-connector.sh saas          # Connect to Camunda SaaS

# Manual Maven commands
mvn clean package                # Build + generate element template
mvn clean verify                 # Run tests with coverage
```

### Docker Deployment

#### Local Docker Cluster (Recommended for Development)

The project includes `docker-compose.test.yml` for a complete local Camunda 8 environment:

```bash
# Start full Camunda 8 stack (Zeebe, Operate, Tasklist, Connectors, Elasticsearch)
docker compose -f docker-compose.test.yml up -d

# Verify services are running
docker ps | grep camunda

# Access components:
# - Operate: http://localhost:8088/operate (demo/demo)
# - Tasklist: http://localhost:8088/tasklist (demo/demo)
# - Zeebe Gateway: localhost:26500
```

#### Deploy Custom Connector to Docker

After building the fat JAR (`mvn clean package`), deploy your RSS Feed Connector:

```bash
# For local Docker cluster (plaintext security)
docker run --rm --name=CustomConnectorInSMCore \
    -v $PWD/target/connector-rssfeed-1.0.0.jar:/opt/app/connector.jar \
    --network=host \
    -e CAMUNDA_CLIENT_BROKER_GATEWAY-ADDRESS=localhost:26500 \
    -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=true \
    -e CAMUNDA_OPERATE_CLIENT_URL=http://localhost:8088 \
    -e CAMUNDA_OPERATE_CLIENT_USERNAME=demo \
    -e CAMUNDA_OPERATE_CLIENT_PASSWORD=demo \
    camunda/connectors-bundle:8.8.1

# For Camunda SaaS cluster
docker run --rm --name=CustomConnectorInSaaS \
    -v $PWD/target/connector-rssfeed-1.0.0.jar:/opt/app/connector.jar \
    -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=false \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_ID='<YOUR_CLIENT_ID>' \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_SECRET='<YOUR_CLIENT_SECRET>' \
    -e CAMUNDA_CLIENT_CLOUD_CLUSTER_ID='<YOUR_CLUSTER_ID>' \
    -e CAMUNDA_CLIENT_CLOUD_REGION='<YOUR_CLUSTER_REGION>' \
    camunda/connectors-bundle:8.8.1
```

**Key Docker configuration notes:**

- Use `--network=host` for local Docker to access localhost services
- Set `CAMUNDA_CLIENT_SECURITY_PLAINTEXT=true` for local development without Keycloak
- The fat JAR is mounted to `/opt/app/connector.jar` inside the container
- Container version must match Connector SDK version (8.8.1)

#### Docker Compose Stack Details

- **orchestration**: Consolidated Zeebe + Operate + Tasklist (port 8088)
- **connectors**: Standard Camunda connectors bundle (port 8086)
- **elasticsearch**: Required for Operate/Tasklist data storage
- **Healthchecks**: Ensures services start in correct order
- **Basic auth**: Username `demo`, password `demo` (no Keycloak)

#### Cleanup Docker Environment

```bash
# Stop and remove all containers/volumes
docker compose -f docker-compose.test.yml down -v

# Stop custom connector
docker stop CustomConnectorInSMCore
```

### Testing Patterns

- **Unit tests**: Use `OutboundConnectorContextBuilder` to mock Camunda context
- **Test resources**: XML files in `src/test/resources/` (test-feed.xml, empty-feed.xml, invalid-feed.xml)
- **Mocking**: HttpClient injection for network isolation
- **Assertions**: AssertJ for fluent assertions

## Key Conventions & Patterns

### Error Handling Strategy

- Custom error codes: `INVALID_URL`, `FETCH_ERROR`, `PARSE_ERROR`, `INVALID_DATE_FORMAT`, `INVALID_DATE_RANGE`
- **Fail-fast validation** in request DTOs with Jakarta validation annotations
- **Graceful degradation** for malformed feed items (log warnings, continue processing)

### Safety Limits & Timeouts

- **Feed size limit**: 500 items max (memory protection)
- **HTTP timeouts**: 10s connect, 30s request (in constructor)
- **Input validation**: maxItems 1-500, proper date format validation

### Date Handling Pattern

- **Input**: ISO8601 strings (`2025-01-01T00:00:00Z`)
- **Parsing**: `OffsetDateTime` with error handling
- **Filtering**: Convert feed dates to `OffsetDateTime` for comparison
- **Sorting**: Most recent first using `Comparator.comparing()`

### Logging Convention

```java
// Always include processInstanceKey when available
LOGGER.info("Executing RSS Feed Connector [processInstanceKey={}] with URL: {}",
    processInstanceKey, connectorRequest.feedUrl());
```

## Integration Points

### Camunda Integration

- **Connector type**: `"io.camunda:rssfeed:1"` (defined in `@OutboundConnector`)
- **Input variables**: `feedUrl`, `maxItems`, `fromDate`, `toDate`
- **Context binding**: Use `context.bindVariables(RssFeedRequest.class)`
- **Job context**: Access `processInstanceKey` via `context.getJobContext()`

### External Dependencies

- **ROME library**: `SyndFeedInput`, `SyndFeed`, `SyndEntry` for RSS parsing
- **Java HTTP Client**: Built-in with timeout configuration, redirect following
- **Validation**: Jakarta validation for input constraints

## Development Environment Setup

### Property Files

- `application-local.properties` - Local Camunda connection
- `application-saas.properties` - SaaS credentials (use `setup-credentials.sh`)
- Spring profiles: `-Dspring.config.additional-location=`

### Maven Configuration Notes

- **Java 21** target (modern language features)
- **Connector SDK**: Version pinned in `version.connectors` property
- **Provided scope**: `connector-core` (supplied by runtime)
- **Test scope**: `connector-test`, `connector-runtime-test`

## Common Patterns When Extending

### Adding New Input Parameters

1. Add field to `RssFeedRequest` record with `@TemplateProperty` annotation
2. Add validation annotations (`@NotBlank`, `@Min`, `@Max`)
3. Update `@OutboundConnector` inputVariables array
4. Element template regenerates automatically on build

### Error Code Addition

1. Define constant in connector class
2. Throw `ConnectorException` with code and descriptive message
3. Document in README.md error codes table
4. Add test case for the error scenario

### Testing New Features

- Create test XML files in `src/test/resources/`
- Use `getTestResourceUrl()` helper for local file URLs
- Mock HttpClient for network call isolation
- Test both happy path and error conditions
