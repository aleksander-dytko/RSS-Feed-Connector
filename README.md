# RSS Feed Connector for Camunda 8

A Camunda 8 Outbound Connector that fetches and parses RSS feed data from a specified URL with optional filtering capabilities.

## Overview

The RSS Feed Connector allows you to integrate RSS/Atom feeds into your Camunda 8 BPMN processes. It fetches feed content, parses it, and returns structured data that can be used in your workflow automation.

### Features

- ✅ Fetch and parse RSS/Atom feeds from any public URL
- ✅ Filter items by date range (fromDate/toDate)
- ✅ Limit the number of returned items (maxItems)
- ✅ Automatic sorting by publication date (most recent first)
- ✅ Graceful handling of incomplete or malformed feed items
- ✅ Comprehensive error handling with specific error codes
- ✅ Support for standard RSS fields: title, link, description, published date, author, categories, GUID

## Build

Package the Connector by running:

```bash
mvn clean package
```

This creates:
- A thin JAR without dependencies
- A fat JAR (uber JAR) with all dependencies, suitable for deployment

The element template is automatically generated during the build process and placed in `element-templates/rss-feed-connector.json`.

## API

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
  "filteredItems": 10
}
```

**Field Descriptions:**
- `items`: Array of RSS feed items after filtering and limiting
- `totalItems`: Total number of items in the original feed
- `filteredItems`: Number of items after applying filters and limits

### Error Codes

| Code                 | Description                                                    | Resolution                                |
|----------------------|----------------------------------------------------------------|-------------------------------------------|
| `INVALID_URL`        | The provided URL is malformed or invalid                       | Check the URL format (must be HTTP/HTTPS) |
| `FETCH_ERROR`        | Failed to fetch the feed (network or server error)             | Verify the URL is accessible              |
| `PARSE_ERROR`        | Failed to parse the feed (invalid RSS/Atom XML)                | Ensure the feed is valid RSS/Atom format  |
| `INVALID_DATE_FORMAT`| The fromDate or toDate is not in ISO8601 format                | Use format: `2025-01-01T00:00:00Z`        |

## Usage Example

### BPMN Configuration

1. Add a Service Task to your BPMN diagram
2. Apply the "RSS Feed Connector" element template
3. Configure the input parameters:

![RSS Feed Process Example](img/process.png)

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

## Test Locally

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

### Test with Local Camunda Runtime

#### Prerequisites

1. [Camunda Desktop Modeler](https://camunda.com/download/modeler/)
2. [Docker Compose](https://docs.docker.com/compose/) version 1.27.0 or later
3. [Docker](https://www.docker.com/products/docker-desktop) version 20.10.16 or later

#### Setup

1. Clone the [Camunda 8 Distributions repository](https://github.com/camunda/camunda-distributions) and navigate to the appropriate version directory:

```bash
git clone https://github.com/camunda/camunda-distributions.git
cd camunda-distributions/versions/camunda-8.8
```

**Note**: This connector requires Camunda 8.8 or later. Replace `camunda-8.8` with your desired version (e.g., `camunda-8.9`).

2. Start Camunda 8 with Docker Compose (lightweight configuration):

```bash
docker compose up -d
```

For the full configuration with all components (Optimize, Console, Web Modeler):

```bash
docker compose -f docker-compose-full.yaml up -d
```

3. Deploy your custom RSS Feed Connector:

**Option 1: Mount connector JAR as volume**

Add the following to your `docker-compose.yaml` under the `connectors` service:

```yaml
volumes:
  - ./target/connector-rssfeed-0.1.0-SNAPSHOT.jar:/opt/app/connector-rssfeed.jar
```

**Option 2: Create custom Docker image**

See the [Connectors documentation](https://github.com/camunda/connectors) for bundling custom connectors.

4. Install the element template:
   - Copy `element-templates/rss-feed-connector.json` to your Desktop Modeler's element templates directory
   - See [Element Templates documentation](https://docs.camunda.io/docs/components/modeler/desktop-modeler/element-templates/configuring-templates/)

5. Deploy a process from Desktop Modeler:
   - Open Desktop Modeler and create a BPMN diagram
   - Add a Service Task and apply the "RSS Feed Connector" template
   - Configure the connector with a feed URL (e.g., `https://feeds.bbci.co.uk/news/rss.xml`)
   - Click the deployment icon and configure:
     - **Cluster endpoint**: `http://localhost:8088`
     - **Authentication**: Select **None** (for lightweight config)
   - Click **Deploy**

6. Access the Camunda components:
   - **Operate**: [http://localhost:8088/operate](http://localhost:8088/operate) - Monitor process instances
   - **Tasklist**: [http://localhost:8088/tasklist](http://localhost:8088/tasklist) - Complete user tasks
   - Login with username: `demo`, password: `demo`

7. Stop Camunda and clean up (from the `camunda-distributions/versions/camunda-8.8` directory):

```bash
docker compose down -v
# or for full configuration:
docker compose -f docker-compose-full.yaml down -v
```

**Note**: The `-v` flag removes all volumes and data. Omit it to preserve your data between restarts.

For more details, see the [Camunda 8 Docker Compose documentation](https://docs.camunda.io/docs/self-managed/quickstart/developer-quickstart/docker-compose/).

### Test with Camunda SaaS

1. Navigate to [Camunda SaaS Console](https://console.camunda.io)
2. Create a cluster and obtain API credentials
3. Configure your `application.properties` with the cluster credentials
4. Upload the element template to Web Modeler
5. Create a BPMN diagram using the RSS Feed Connector
6. Deploy and run your process

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

## Support

For questions or issues:
- Review the [Camunda Connector SDK documentation](https://docs.camunda.io/docs/components/connectors/custom-built-connectors/connector-sdk/)
- Check the [Element Templates documentation](https://docs.camunda.io/docs/components/modeler/element-templates/defining-templates/)
- Open an issue in this repository
