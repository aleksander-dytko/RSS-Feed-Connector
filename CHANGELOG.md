# Changelog

All notable changes to the RSS Feed Connector for Camunda 8 will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2025-10-30

### Fixed

- Fixed GitHub Actions CI/CD workflows for automated releases
- Fixed release notes generation to properly handle version tags
- Fixed integration tests to handle empty state gracefully
- Fixed Elasticsearch version compatibility in docker-compose setup
- Removed unnecessary DEFAULT_MAX_ITEMS constant and cleaned up element template generation

### Changed

- Code formatting improvements for consistency
- Improved error handling in integration tests

## [1.0.0] - 2025-10-27

### Added

- Initial stable release of RSS Feed Connector for Camunda 8
- **Core Features**:
  - Fetch and parse RSS/Atom feeds from any public URL
  - Filter feed items by date range (fromDate/toDate)
  - Limit number of returned items (maxItems)
  - Automatic sorting by publication date (most recent first)
  - Support for FEEL expressions in date filters (e.g., `today()`, `now()`)
  
- **Feed Processing**:
  - Parse standard RSS 2.0 feeds
  - Parse Atom feeds
  - Extract feed metadata (title, description, link, last update time)
  - Extract item fields: title, link, description, publishedDate, author, categories, GUID
  - Graceful handling of incomplete or malformed feed items
  - Safety limit of 500 items per feed to prevent memory issues

- **Error Handling**:
  - Comprehensive error handling with specific error codes:
    - `INVALID_URL` - Malformed or unsupported URL
    - `FETCH_ERROR` - Network or server errors
    - `PARSE_ERROR` - Invalid RSS/Atom XML
    - `INVALID_DATE_FORMAT` - Malformed date strings
    - `INVALID_DATE_RANGE` - Invalid date range (fromDate > toDate)
  - Detailed error messages for troubleshooting

- **Configuration**:
  - Built-in timeouts (10s connection, 30s request)
  - Default maxItems of 10
  - Support for HTTP, HTTPS, and file:// URLs (file:// for testing)
  - Automatic User-Agent header: `Camunda-RSS-Feed-Connector/1.0`

- **Testing**:
  - Comprehensive unit test suite (21 tests)
  - Integration tests with live RSS feeds
  - Docker-based testing environment
  - GitHub Actions CI/CD pipeline with automated testing

- **Documentation**:
  - Complete API documentation in README
  - Usage examples for BPMN processes
  - Sample BPMN file (`fetch_rss_example.bpmn`)
  - Element template for Camunda Modeler
  - Security best practices guide (SECURITY.md)
  - Contributor guidelines (CONTRIBUTING.md)

### Technical Details

- **Dependencies**:
  - Camunda Connector SDK 8.8.1
  - Rome Tools 2.1.0 for RSS/Atom parsing
  - Java 21
  - Jakarta Validation for input validation

- **Architecture**:
  - Follows Camunda Connector SDK pattern
  - Stateless connector design
  - Supports both self-hosted and SaaS deployments
  - Element template auto-generated from Java annotations

### Known Limitations

- Maximum 500 items processed per feed (safety limit)
- No authentication support (public feeds only)
- No feed caching (always fetches fresh data)
- Date-only format assumes UTC timezone
- Items without dates are included in results but sorted to the end

[1.0.0]: https://github.com/aleksander-dytko/RSS-Feed-Connector/releases/tag/v1.0.0

