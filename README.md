<div align="center">
  <img src="assets/icon.svg" alt="RSS Feed Connector Icon" width="120" height="120">
  <h1>RSS Feed Connector for Camunda 8</h1>
  
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
  
  <p><strong>Integrate RSS/Atom feeds into your Camunda 8 BPMN processes with powerful filtering capabilities</strong></p>
</div>

---

## Overview

The RSS Feed Connector allows you to integrate RSS/Atom feeds into your Camunda 8 BPMN processes. It fetches feed content, parses it, and returns structured data that can be used in your workflow automation.

## 📋 Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Build](#build)
- [API Reference](#api-reference)
  - [Input Parameters](#input-parameters)
  - [Output Structure](#output-structure)
  - [Error Codes](#error-codes)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Visual Guide](#visual-guide)
- [Testing](#testing-the-connector)
- [Deployment](#hosting-custom-connectors)
- [Troubleshooting](#troubleshooting)
- [Technical Details](#technical-details)
- [Contributing](#contributing)
- [License](#license)

### Features

- ✅ Fetch and parse RSS/Atom feeds from any public URL
- ✅ Filter items by date range (fromDate/toDate)
- ✅ Limit the number of returned items (maxItems)
- ✅ Automatic sorting by publication date (most recent first)
- ✅ Graceful handling of incomplete or malformed feed items
- ✅ Comprehensive error handling with specific error codes
- ✅ Support for standard RSS fields: title, link, description, published date, author, categories, GUID

## Quick Start

The connector includes a convenient runner script for different environments:

```bash
# Run connector connected to Camunda SaaS
./run-connector.sh saas

# Run connector connected to local Camunda Platform
./run-connector.sh local

# Run unit tests
./run-connector.sh test
```

For detailed deployment instructions, see the [Hosting Custom Connectors](#hosting-custom-connectors) section.

## Build

Package the Connector by running:

```bash
mvn clean package
```

This creates:
- A thin JAR without dependencies
- A fat JAR (uber JAR) with all dependencies, suitable for deployment

The element template is automatically generated during the build process and placed in `element-templates/rss-feed-connector.json`.

## API Reference

### Input Parameters

| Name     | Type    | Required | Description                                                                 | Example                                  |
|----------|---------|----------|-----------------------------------------------------------------------------|------------------------------------------|
| feedUrl  | String  | Yes      | The URL of the RSS feed to fetch                                            | `https://feeds.bbci.co.uk/news/rss.xml`  |
| maxItems | Integer | No       | Maximum number of items to return (default: 10, max: 500)                   | `10`                                     |
| fromDate | String  | No       | Filter items published on or after this date (ISO8601 format)               | `2025-01-01T00:00:00Z`                   |
| toDate   | String  | No       | Filter items published on or before this date (ISO8601 format)              | `2025-12-31T23:59:59Z`                   |

### Output Structure

```json
{
  "items": [
    {
      "title": "Breaking News: Example Title",
      "link": "https://example.com/article",
      "description": "Article summary or description",
      "publishedDate": "2025-10-26T10:30:00Z",
      "author": "John Doe",
      "categories": ["Technology", "News"],
      "guid": "https://example.com/article"
    }
  ],
  "totalItems": 150,
  "filteredItems": 10,
  "metadata": {
    "title": "BBC News - Home",
    "description": "BBC News RSS feed",
    "link": "https://www.bbc.co.uk/news/",
    "lastBuildDate": "2025-10-26T15:30:00Z"
  }
}
```

**Field Descriptions:**
- `items`: Array of RSS feed items after filtering and limiting
- `totalItems`: Total number of items in the original feed before filtering (max 500, see [Limits](#limits))
- `filteredItems`: Number of items after applying filters and limits
- `metadata`: Information about the RSS feed itself
  - `title`: Feed title (e.g., "BBC News - Home")
  - `description`: Feed description
  - `link`: Link to the feed's website
  - `lastBuildDate`: When the feed was last updated (ISO 8601 format)

### Error Codes

| Code                 | Description                                                    | Resolution                                |
|----------------------|----------------------------------------------------------------|-------------------------------------------|
| `INVALID_URL`        | The provided URL is malformed or invalid                       | Check the URL format (must be HTTP/HTTPS) |
| `FETCH_ERROR`        | Failed to fetch the feed (network or server error)             | Verify the URL is accessible              |
| `PARSE_ERROR`        | Failed to parse the feed (invalid RSS/Atom XML)                | Ensure the feed is valid RSS/Atom format  |
| `INVALID_DATE_FORMAT`| The fromDate or toDate is not in ISO8601 format                | Use format: `2025-01-01T00:00:00Z`        |
| `INVALID_DATE_RANGE` | The fromDate is after toDate                                   | Ensure fromDate ≤ toDate                  |

## Configuration

### Timeout Settings

The connector includes built-in timeout protection to prevent hanging connections:

- **Connection Timeout**: 10 seconds - Maximum time to establish a connection to the RSS feed server
- **Request Timeout**: 30 seconds - Maximum time to complete the entire request (including data transfer)

These timeouts protect against:
- Slow or unresponsive servers
- Network issues
- Malicious servers attempting to keep connections open

**Note**: Timeouts are not currently configurable but may be made adjustable via environment variables in future versions.

### Limits

#### Feed Size Limit (500 Items)

For memory safety, the connector processes a maximum of **500 items** from any RSS feed. This limit applies before any filtering:

- If a feed contains more than 500 items, only the first 500 are processed
- A warning is logged when truncation occurs: `Feed contains X items, but only 500 items will be processed`
- The `totalItems` field in the response reflects the truncated count (max 500)

**Recommendations:**
- Use date filters (`fromDate`, `toDate`) to reduce the result set at the source
- Monitor logs for truncation warnings
- Consider using feed pagination if available from the source

#### MaxItems Parameter Limit

- **Minimum**: 1
- **Maximum**: 500
- **Default**: 10

The `maxItems` parameter controls how many items are returned **after** filtering. This is applied after date range filtering and sorting.

## Usage Examples

### Basic Usage

Fetch the 5 most recent items from a feed:

```json
{
  "feedUrl": "https://feeds.bbci.co.uk/news/rss.xml",
  "maxItems": 5
}
```

### Using Feed Metadata

Access feed metadata in your BPMN process to display or store feed information:

**BPMN Configuration:**
```json
{
  "feedUrl": "https://techcrunch.com/feed/",
  "maxItems": 10
}
```

**Accessing Metadata in Process Variables:**
```javascript
// Get feed title
= feedResult.metadata.title

// Get feed description
= feedResult.metadata.description

// Get feed last update time
= feedResult.metadata.lastBuildDate

// Display feed info in user task
= "Latest from " + feedResult.metadata.title + " (" + feedResult.filteredItems + " items)"
```

### Date Filtering Examples

#### Filter by Date Range

Get articles from a specific week:

```json
{
  "feedUrl": "https://feeds.bbci.co.uk/news/rss.xml",
  "maxItems": 50,
  "fromDate": "2025-10-20T00:00:00Z",
  "toDate": "2025-10-26T23:59:59Z"
}
```

#### Recent Items Only (using FEEL expressions)

Get items from the last 7 days using FEEL's `today()` function:

```json
{
  "feedUrl": "https://feeds.bbci.co.uk/news/rss.xml",
  "maxItems": 20,
  "fromDate": "=today() - duration(\"P7D\")"
}
```

#### Items from Today

```json
{
  "feedUrl": "https://www.theguardian.com/world/rss",
  "maxItems": 30,
  "fromDate": "=today()"
}
```

### Processing Results in BPMN

**Example: Send Email with Latest News**

```javascript
// In an email task, build a summary of the latest articles
= "Daily News Summary\n\n" + 
  "Source: " + feedResult.metadata.title + "\n" +
  "Latest " + feedResult.filteredItems + " articles:\n\n" +
  for item in feedResult.items return 
    "• " + item.title + "\n  " + item.link + "\n"
```

**Example: Filter by Category in Process**

```javascript
// Filter items that have "Technology" category
= feedResult.items[item.categories contains "Technology"]

// Count items in a specific category
= count(feedResult.items[item.categories contains "Business"])
```

**Example: Store First Article Details**

```javascript
// Get first article title
= feedResult.items[1].title

// Get first article link
= feedResult.items[1].link

// Get first article published date
= feedResult.items[1].publishedDate
```

### BPMN Configuration

1. Add a Service Task to your BPMN diagram
2. Apply the "RSS Feed Connector" element template
3. Configure the input parameters:

![RSS Feed Process Example](assets/screenshots/diagram.png)

```json
{
  "feedUrl": "https://feeds.bbci.co.uk/news/rss.xml",
  "maxItems": 5,
  "fromDate": "2025-10-01T00:00:00Z"
}
```

4. Map the output to a process variable:
   - Result variable: `feedResult`

5. Access the results in subsequent tasks:
   - `feedResult.items[0].title` - First item's title
   - `feedResult.filteredItems` - Number of items returned

### Sample RSS Feeds for Testing

Here are some publicly available RSS feeds you can use for testing:

| Source          | URL                                                          |
|-----------------|--------------------------------------------------------------|
| BBC News        | `https://feeds.bbci.co.uk/news/rss.xml`                      |
| The Guardian    | `https://www.theguardian.com/world/rss`                      |
| TechCrunch      | `https://techcrunch.com/feed/`                               |
| Hacker News     | `https://news.ycombinator.com/rss`                           |
| NASA Breaking   | `https://www.nasa.gov/rss/dyn/breaking_news.rss`             |

## Visual Guide

### Configure in Camunda Modeler

The RSS Feed Connector appears in Camunda Modeler with a custom icon and intuitive configuration panel:

<div align="center">
  <img src="assets/screenshots/Modeler.png" alt="RSS Feed Connector in Camunda Modeler" width="800">
  <p><em>Configure the RSS Feed Connector in Camunda Modeler with the custom element template</em></p>
</div>

**Configuration Steps:**
1. Add a Service Task to your BPMN diagram
2. Apply the "RSS Feed Connector" element template
3. Configure input parameters (feedUrl, maxItems, fromDate, toDate)
4. Set the result variable name (e.g., `feedResult`)

### Monitor in Camunda Operate

Track your RSS Feed Connector executions in Camunda Operate:

<div align="center">
  <img src="assets/screenshots/Operate.png" alt="RSS Feed Connector in Camunda Operate" width="800">
  <p><em>Monitor RSS Feed Connector execution and results in Camunda Operate</em></p>
</div>

**What you can see in Operate:**
- Process instance execution status
- Input parameters used (feedUrl, filters)
- Output results (items fetched, metadata)
- Error details if the connector fails

## Testing the Connector

### Run Unit Tests

```bash
mvn clean verify
```

The test suite includes:
- Valid RSS feed parsing
- Empty feed handling
- Invalid URL and malformed XML error handling
- Date filtering (fromDate, toDate, date range)
- MaxItems limiting
- Sorting verification
- Missing field handling

## Deployment

### Hosting Custom Connectors

Based on the [Camunda documentation](https://docs.camunda.io/docs/components/connectors/custom-built-connectors/host-custom-connectors/), there are several ways to host your custom RSS Feed Connector. We've tested both local Docker and SaaS approaches successfully.

#### Prerequisites

1. **Build the connector**: First, build the fat JAR with all dependencies:
   ```bash
   mvn clean package
   ```
   This creates `target/connector-rssfeed-0.1.0-SNAPSHOT.jar` (the fat JAR with dependencies).

2. **Docker**: Ensure Docker is installed and running
3. **Camunda Cluster**: Either a local Docker cluster or SaaS cluster

### Approach 1: Local Docker Cluster (without Keycloak)

This approach uses Docker with plaintext security for local development.

#### Setup Local Camunda Cluster

1. Clone the [Camunda 8 Distributions repository](https://github.com/camunda/camunda-distributions):
   ```bash
   git clone https://github.com/camunda/camunda-distributions.git
   cd camunda-distributions/docker-compose/versions/camunda-8.8
   ```

2. Start Camunda 8 with Docker Compose:
   ```bash
   docker compose up -d
   ```

3. Verify the cluster is running:
   ```bash
   docker ps | grep camunda
   ```
   You should see containers for `orchestration` and `connectors`.

#### Deploy Your Custom Connector

Run your RSS Feed Connector using the connectors-bundle Docker image:

```bash
docker run --rm --name=CustomConnectorInSMCore \
    -v $PWD/target/connector-rssfeed-0.1.0-SNAPSHOT.jar:/opt/app/connector.jar \
    --network=host \
    -e CAMUNDA_CLIENT_BROKER_GATEWAY-ADDRESS=localhost:26500 \
    -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=true \
    -e CAMUNDA_OPERATE_CLIENT_URL=http://localhost:8088 \
    -e CAMUNDA_OPERATE_CLIENT_USERNAME=demo \
    -e CAMUNDA_OPERATE_CLIENT_PASSWORD=demo \
    camunda/connectors-bundle:8.8.1
```

**Key points:**
- `--network=host`: Allows the connector to connect to localhost services
- `CAMUNDA_CLIENT_SECURITY_PLAINTEXT=true`: Uses plaintext security for local development
- `camunda/connectors-bundle:8.8.1`: Uses the correct version tag

#### Verify Success

In the logs, you should see:
```
Starting job worker: JobWorkerValue{type='io.camunda:rssfeed:1', name='RssFeedConnector', ...}
```

This confirms your RSS Feed Connector is loaded and ready to process jobs.

#### Access Camunda Components

- **Operate**: [http://localhost:8088/operate](http://localhost:8088/operate) - Monitor process instances
- **Tasklist**: [http://localhost:8088/tasklist](http://localhost:8088/tasklist) - Complete user tasks
- Login with username: `demo`, password: `demo`

### Approach 2: Camunda SaaS Cluster

This approach connects your custom connector to a Camunda SaaS cluster.

#### Prerequisites

1. **SaaS Cluster**: Create a cluster at [Camunda SaaS Console](https://console.camunda.io)
2. **API Credentials**: Create a client with `zeebe` scope and copy the credentials
3. **Update Configuration**: Update `src/test/resources/application.properties` with your SaaS credentials

#### Deploy Your Custom Connector

Run your RSS Feed Connector connected to SaaS:

```bash
docker run --rm --name=CustomConnectorInSaaS \
    -v $PWD/target/connector-rssfeed-0.1.0-SNAPSHOT.jar:/opt/app/connector.jar \
    -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=false \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_ID='<YOUR_CLIENT_ID>' \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_SECRET='<YOUR_CLIENT_SECRET>' \
    -e CAMUNDA_CLIENT_CLOUD_CLUSTER_ID='<YOUR_CLUSTER_ID>' \
    -e CAMUNDA_CLIENT_CLOUD_REGION='<YOUR_CLUSTER_REGION>' \
    -e CAMUNDA_OPERATE_CLIENT_URL='https://<region>.operate.camunda.io/<cluster-id>' \
    camunda/connectors-bundle:8.8.1
```

**Replace the following with your actual SaaS credentials:**
- `<YOUR_CLUSTER_ID>`: Your cluster ID from the SaaS console
- `<YOUR_CLIENT_ID>`: Your client ID
- `<YOUR_CLIENT_SECRET>`: Your client secret
- `<YOUR_CLUSTER_REGION>`: Your cluster region (e.g., `bru-2`, `us-1`)

#### Verify Success

In the logs, you should see:
```
Starting job worker: JobWorkerValue{type='io.camunda:rssfeed:1', name='RssFeedConnector', ...}
```

This confirms your RSS Feed Connector is loaded and ready to process jobs. The connector will successfully authenticate with the SaaS cluster and start polling for jobs.

### Approach 3: Local Development with Spring Boot

For development and testing, you can also run the connector directly with Spring Boot:

#### Local Development

1. **Start Local Camunda Cluster** (as described in Approach 1)

2. **Run the connector**:
   ```bash
   ./run-connector.sh local
   ```
   
   Or manually:
   ```bash
   mvn exec:java -Dspring.config.additional-location=src/test/resources/application-local.properties
   ```

#### SaaS Development

1. **Update SaaS credentials** in `src/test/resources/application.properties`

2. **Run the connector**:
   ```bash
   ./run-connector.sh saas
   ```
   
   Or manually:
   ```bash
   mvn exec:java
   ```

### Using the Connector in BPMN Processes

1. **Install Element Template**:
   - Copy `element-templates/rss-feed-connector.json` to your Desktop Modeler's element templates directory
   - Or upload it to Web Modeler

2. **Create BPMN Process**:
   - Add a Service Task to your BPMN diagram
   - Apply the "RSS Feed Connector" element template
   - Configure the input parameters (feedUrl, maxItems, fromDate, toDate)

3. **Deploy and Run**:
   - Deploy your process to the cluster
   - Start a process instance
   - Monitor execution in Operate

### Cleanup

#### Stop Local Cluster
```bash
cd camunda-distributions/docker-compose/versions/camunda-8.8
docker compose down -v
```

#### Stop Connector Containers
```bash
docker stop CustomConnectorInSMCore  # For local Docker approach
docker stop CustomConnectorInSaaS    # For SaaS approach
```

## Troubleshooting

### Common Issues

**1. Authentication Errors (SaaS)**
- **Symptom**: `401 Unauthorized` or authentication failures
- **Solution**: Ensure your SaaS credentials are correct and use the proper environment variable names:
  - `CAMUNDA_CLIENT_AUTH_CLIENT_ID` (not `CAMUNDA_CLIENT_CLOUD_CLIENT-ID`)
  - `CAMUNDA_CLIENT_AUTH_CLIENT_SECRET` (not `CAMUNDA_CLIENT_CLOUD_CLIENT-SECRET`)
  - `CAMUNDA_CLIENT_CLOUD_CLUSTER_ID` (not `CAMUNDA_CLIENT_CLOUD_CLUSTER-ID`)

**2. Network Issues (Local Docker)**
- **Symptom**: Cannot connect to localhost services
- **Solution**: Use `--network=host` for local Docker approach

**3. Version Mismatch**
- **Symptom**: Connector fails to load or behaves unexpectedly
- **Solution**: Ensure you're using `camunda/connectors-bundle:8.8.1` or compatible version

**4. JAR Not Found**
- **Symptom**: Error loading connector JAR
- **Solution**: Ensure the connector JAR is built (`mvn clean package`) and the path in the Docker volume mount is correct

### RSS Feed Specific Issues

**5. Feed Timeout Errors**
- **Symptom**: `FETCH_ERROR` with timeout message
- **Cause**: Feed server is slow or unresponsive
- **Solution**: 
  - Verify the feed URL is accessible from your network
  - Check if the feed server has rate limiting
  - The connector has 10s connection timeout and 30s request timeout
  - Consider using a different feed source if the issue persists

**6. Invalid Feed Format**
- **Symptom**: `PARSE_ERROR` - "Failed to parse RSS feed"
- **Cause**: Feed is not valid RSS/Atom XML
- **Solution**:
  - Validate the feed URL in a browser or RSS reader
  - Check if the feed returns HTML error pages instead of XML
  - Ensure the feed follows RSS 2.0 or Atom standards

**7. Empty Results Despite Data in Feed**
- **Symptom**: `filteredItems: 0` when feed has content
- **Possible Causes**:
  - Date filters are too restrictive (fromDate/toDate)
  - Date range is inverted (fromDate > toDate) - now returns `INVALID_DATE_RANGE` error
- **Solution**:
  - Check your date filters are correct
  - Verify feed items have publication dates
  - Remove date filters temporarily to test

**8. Truncated Results (Large Feeds)**
- **Symptom**: `totalItems: 500` but feed has more items
- **Cause**: Safety limit of 500 items (see [Limits](#limits))
- **Solution**:
  - Use date filters to reduce the result set: `fromDate`, `toDate`
  - Check connector logs for truncation warning
  - Consider fetching feed in smaller date ranges

**9. Invalid Date Format**
- **Symptom**: `INVALID_DATE_FORMAT` error
- **Cause**: Date not in ISO 8601 format
- **Solution**: Use correct format:
  - Standard: `2025-01-01T00:00:00Z`
  - Date only: `2025-01-01` (assumes start of day UTC)
  - FEEL expression: `=today()` or `=now()`

**10. Missing Metadata Fields**
- **Symptom**: `metadata` fields are null
- **Cause**: Feed doesn't provide optional metadata
- **Solution**: This is normal - handle null values in your process:
  ```javascript
  = if feedResult.metadata.title != null 
    then feedResult.metadata.title 
    else "Unknown Feed"
  ```

### Debugging

**Check Connector Logs**

For detailed information about what's happening:
```bash
docker logs CustomConnectorInSMCore  # For local Docker
docker logs CustomConnectorInSaaS    # For SaaS
docker logs -f CustomConnectorInSaaS  # Follow logs in real-time
```

**Look for Key Log Messages:**

✅ Success indicators:
```
Starting job worker: JobWorkerValue{type='io.camunda:rssfeed:1', name='RssFeedConnector', ...}
Executing RSS Feed Connector [processInstanceKey=123456] with URL: ...
Parsed 35 items, filtered to 10 items
```

⚠️ Warning indicators:
```
Feed contains 1000 items, but only 500 items will be processed due to safety limit
```

❌ Error indicators:
```
ERROR ... Failed to fetch RSS feed from URI: ... Network or server error: ...
ERROR ... Failed to parse RSS feed. The content may not be valid RSS/Atom XML: ...
```

**Enable Debug Logging**

Add to your connector startup:
```bash
-e LOGGING_LEVEL_IO_CAMUNDA_CONNECTOR_RSSFEED=DEBUG
```

This will show additional details about feed processing, filtering, and item conversion.

**Test Feed Manually**

Before debugging the connector, verify the feed works:
```bash
curl -v "https://feeds.bbci.co.uk/news/rss.xml"
```

Check for:
- HTTP 200 status code
- Content-Type: application/rss+xml or application/xml
- Valid XML response (not HTML error page)


## Element Template

The element template is automatically generated from the connector input class using the [Element Template Generator](https://github.com/camunda/connectors/tree/main/element-template-generator/core).

To regenerate the template:

```bash
mvn clean package
```

The generated template: `element-templates/rss-feed-connector.json`

## Technical Details

### Dependencies

- **Rome Tools (2.1.0)**: RSS/Atom feed parsing library
- **Camunda Connector SDK (8.8.1)**: Connector framework
- **Jakarta Validation**: Input parameter validation
- **SLF4J**: Logging

### Architecture

The connector follows the Camunda Connector SDK pattern:

```
io.camunda.connector.rssfeed/
├── RssFeedConnectorFunction.java    # Main connector logic
└── dto/
    ├── RssFeedRequest.java           # Input parameters with validation
    ├── RssFeedResult.java            # Output structure
    └── RssFeedItem.java              # Individual feed item
```

### Processing Pipeline

1. **Validation**: URL and date format validation
2. **Fetch**: HTTP request to feed URL
3. **Parse**: XML parsing using Rome Tools
4. **Filter**: Apply date range filters
5. **Sort**: Order by publishedDate descending
6. **Limit**: Apply maxItems limit
7. **Return**: Structured result with metadata

## Future Extensions

Potential enhancements for future versions:

- **Authentication Support**: Basic Auth and custom headers for protected feeds
- **Atom Feed Optimization**: Enhanced Atom-specific field mapping
- **Pagination**: Support for feeds with pagination
- **Caching**: Optional feed caching to reduce network requests
- **Content Filtering**: Keyword-based filtering of feed content
- **Multiple Feeds**: Fetch and merge multiple feeds in one connector call
- **Webhooks**: Subscribe to feed updates instead of polling

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Security

This project implements secure credential management to protect sensitive data. See [SECURITY.md](SECURITY.md) for detailed security practices and setup instructions.

**Quick Setup:**
```bash
./setup-credentials.sh
# Edit src/test/resources/application-local.properties with your credentials
./run-connector.sh saas
```

## Support

For questions or issues:
- Review the [Camunda Connector SDK documentation](https://docs.camunda.io/docs/components/connectors/custom-built-connectors/connector-sdk/)
- Check the [Element Templates documentation](https://docs.camunda.io/docs/components/modeler/element-templates/defining-templates/)
- See [SECURITY.md](SECURITY.md) for credential management
- Open an issue in this repository
