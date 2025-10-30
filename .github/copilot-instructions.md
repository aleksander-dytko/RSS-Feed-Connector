# Copilot Instructions for RSS Feed Connector

## Project Overview

A **Camunda 8 Outbound Connector** that fetches/parses RSS/Atom feeds via ROME library. Integrates RSS into BPMN workflows with date filtering and item limits.

## Architecture & Critical Patterns

### Connector Structure (Camunda SDK Pattern)

**Main class**: `RssFeedConnectorFunction` implements `OutboundConnectorFunction`

- `@OutboundConnector(type = "io.camunda:rssfeed:1")` - defines connector type and input variables
- `@ElementTemplate` - generates Camunda Modeler UI template at build time
- Constructor injection for `HttpClient` (enables test mocking)

**DTOs** (Java records in `dto/` package):

- `RssFeedRequest` - uses `@TemplateProperty` for UI generation + Jakarta validation
- `RssFeedResult`, `RssFeedItem`, `FeedMetadata` - immutable output structures

**Service registration**: `META-INF/services/io.camunda.connector.api.outbound.OutboundConnectorFunction` file contains fully qualified class name for auto-discovery

### Auto-Generated Element Template

**DO NOT manually edit** `element-templates/rss-feed-connector.json` - regenerated on every `mvn package`

- To add inputs: Add field to `RssFeedRequest` with `@TemplateProperty` + update `@OutboundConnector.inputVariables`
- To change UI: Modify annotation properties (label, description, group, defaultValue)
- Build triggers `element-template-generator-maven-plugin`

## Critical Development Workflows

### Essential Commands (via `./run-connector.sh`)

```bash
./run-connector.sh test   # Unit tests only
./run-connector.sh local  # Runs against local docker-compose cluster
./run-connector.sh saas   # Connects to SaaS (needs application-saas.properties)
```

**What happens internally**: Script uses Spring Boot + profile selection to inject Camunda client config from `src/test/resources/application-{profile}.properties`

### Maven Build (generates element template)

```bash
mvn clean package  # Creates fat JAR + element-templates/rss-feed-connector.json
mvn verify         # Runs tests WITHOUT integration tests
```

### Testing Strategy

**Unit tests** (`RssFeedConnectorFunctionTest`):

- Mock `OutboundConnectorContext` via `OutboundConnectorContextBuilder`
- Inject mock `HttpClient` in constructor for network isolation
- Feed test data from `src/test/resources/{test,empty,invalid}-feed.xml` using `file://` URLs

**Integration tests** (GitHub Actions only):

- Spins up full Camunda stack via `docker-compose.test.yml`
- Deploys connector JAR to `camunda/connectors-bundle:8.8.1` container
- Executes `.github/scripts/integration-test.sh` which deploys BPMN and verifies execution
- **Not run locally by default** (requires Docker + 120s startup time)

### Local Docker Development

**CRITICAL**: Container version (`8.8.1`) MUST match `<version.connectors>` in pom.xml

```bash
# Start cluster (includes Zeebe, Operate, Tasklist, Connectors runtime)
docker compose -f docker-compose.test.yml up -d

# Deploy YOUR connector as sidecar
docker run -d --name=rssfeed-connector --network=host \
  -v $PWD/target/connector-rssfeed-*.jar:/opt/app/connector.jar \
  -e CAMUNDA_CLIENT_BROKER_GATEWAY_ADDRESS=localhost:26500 \
  -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=true \
  camunda/connectors-bundle:8.8.1

# Access UI: http://localhost:8088/operate (demo/demo)
```

**Why `--network=host`**: Connector needs to reach `localhost:26500` (Zeebe gateway) from within container

### Deploying to Camunda SaaS

**Setup credentials** (run once):

```bash
./setup-credentials.sh  # Prompts for SaaS credentials, creates application-saas.properties
```

**Docker deployment to SaaS**:

```bash
# After building: mvn clean package
docker run -d --name=rssfeed-connector-saas \
  -v $PWD/target/connector-rssfeed-*.jar:/opt/app/connector.jar \
  -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=false \
  -e CAMUNDA_CLIENT_AUTH_CLIENT_ID='<YOUR_CLIENT_ID>' \
  -e CAMUNDA_CLIENT_AUTH_CLIENT_SECRET='<YOUR_CLIENT_SECRET>' \
  -e CAMUNDA_CLIENT_CLOUD_CLUSTER_ID='<YOUR_CLUSTER_ID>' \
  -e CAMUNDA_CLIENT_CLOUD_REGION='<YOUR_CLUSTER_REGION>' \
  camunda/connectors-bundle:8.8.1
```

**Key differences from local**:

- NO `--network=host` (connects via internet)
- `CAMUNDA_CLIENT_SECURITY_PLAINTEXT=false` (uses OAuth)
- Requires cluster credentials from Camunda Console
- Container version MUST match SDK version (8.8.1)

## Project-Specific Conventions

### Error Handling (Custom Exception Codes)

All errors throw `ConnectorException` with standardized codes:

- `INVALID_URL` - Malformed URL or unsupported scheme (not http/https/file)
- `FETCH_ERROR` - Network/HTTP issues (timeouts, 4xx/5xx responses)
- `PARSE_ERROR` - Invalid RSS/Atom XML structure
- `INVALID_DATE_FORMAT` - Date string doesn't match ISO8601 or FEEL output formats
- `INVALID_DATE_RANGE` - fromDate > toDate (cross-field validation)

**Pattern**: Fail-fast validation in DTOs (Jakarta), graceful degradation in feed parsing (log + continue)

### Safety Limits (Memory Protection)

- `SAFETY_LIMIT_ITEMS = 500` - Hard cap on feed entries processed (warns if truncated)
- `HTTP_CONNECT_TIMEOUT = 10s`, `HTTP_REQUEST_TIMEOUT = 30s` - Set in constructor
- `maxItems` input: 1-500 range enforced by `@Min/@Max` validation

### Date Handling (FEEL Expression Support)

**Input formats accepted**:

- ISO8601 with timezone: `2025-01-01T00:00:00Z`
- Date-only (from FEEL `today()`): `2025-01-01` → converted to UTC midnight
- With timezone identifier: `2025-10-25T12:20:31.434Z[GMT]` → strips `[GMT]` before parsing

**Implementation**: `RssFeedRequest.parseDate()` tries `OffsetDateTime.parse()`, falls back to `LocalDate.parse()` + UTC conversion

### Logging Convention

```java
// ALWAYS include processInstanceKey when available (for correlation in Operate)
LOGGER.info("Executing RSS Feed Connector [processInstanceKey={}] with URL: {}, maxItems: {}",
    processInstanceKey, connectorRequest.feedUrl(), connectorRequest.getMaxItemsOrDefault());
```

## Extension Patterns

### Adding Input Parameters

1. Add field to `RssFeedRequest` record with `@TemplateProperty` (example: `feedUrl`)
2. Add Jakarta validation: `@NotBlank`, `@Min`, `@Max`, etc.
3. **CRITICAL**: Update `@OutboundConnector(inputVariables = {"feedUrl", "maxItems", ...})` array
4. Rebuild - element template auto-generates with new UI field

### Release Process (Tag-Based)

```bash
# 1. Update version in pom.xml (e.g., 1.0.1 → 1.0.2)
# 2. Update CHANGELOG.md with new ## [X.Y.Z] section
# 3. Commit changes
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin main vX.Y.Z
# GitHub Actions auto-creates release with CHANGELOG extraction
```

**CHANGELOG extraction** (`.github/workflows/ci.yml`):

- Uses awk pattern: `/^## \[${VERSION}\]/ {found=1; next} found && /^## \[/ {exit} found`
- Skips version header, extracts until next `## [` section
- **Must follow Keep a Changelog format** with `## [X.Y.Z] - YYYY-MM-DD` headers
