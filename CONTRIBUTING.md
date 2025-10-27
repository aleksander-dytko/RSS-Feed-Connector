# Contributing to RSS Feed Connector

Thank you for your interest in contributing to the RSS Feed Connector for Camunda 8! This document provides guidelines for contributing.

## Development Setup

### Prerequisites

- **Java 21** or later
- **Maven 3.6+**
- **Docker** and **Docker Compose** (for integration testing)
- **Git**

### Building the Project

```bash
# Clone the repository
git clone https://github.com/aleksander-dytko/RSS-Feed-Connector.git
cd RSS-Feed-Connector

# Build the project
mvn clean package

# This creates:
# - target/connector-rssfeed-{version}.jar (fat JAR with all dependencies)
# - element-templates/rss-feed-connector.json (element template for Modeler)
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with verbose output
mvn test -X

# Run specific test class
mvn test -Dtest=RssFeedConnectorFunctionTest

# Run a specific test method
mvn test -Dtest=RssFeedConnectorFunctionTest#shouldParseValidRssFeed

# Run tests including integration tests (requires Docker)
mvn verify
```

### Testing with Local Camunda Cluster

The project includes convenience scripts for testing the connector with a local Camunda cluster:

```bash
# Run tests only
./run-connector.sh test

# Run connector connected to local Docker cluster
./run-connector.sh local

# Run connector connected to Camunda SaaS
./run-connector.sh saas
```

For detailed setup instructions, see [README.md](README.md#approaches) and [SECURITY.md](SECURITY.md).

## Development Guidelines

### Code Style

- Use Java 21 features (record classes, pattern matching, etc.)
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) conventions
- Use meaningful variable and method names
- Add JavaDoc for public classes and methods
- Keep methods focused and small (single responsibility principle)

### Adding New Features

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**:
   - Update code in `src/main/java`
   - Add unit tests in `src/test/java`
   - Update documentation if needed

3. **Run tests and verify**:
   ```bash
   mvn clean verify
   ```

4. **Test with local cluster** (optional but recommended):
   ```bash
   ./run-connector.sh local
   ```

5. **Commit your changes** with a clear message:
   ```bash
   git commit -m "Add feature: brief description"
   ```

6. **Push and create a Pull Request**:
   ```bash
   git push origin feature/your-feature-name
   ```

### Writing Tests

- **Unit Tests**: For individual classes and methods
  - Place in `src/test/java`
  - Use JUnit 5 and AssertJ
  - Mock external dependencies with Mockito
  - Test both success and error cases

- **Integration Tests**: For end-to-end testing
  - Use test RSS feeds located in `src/test/resources`
  - Test actual HTTP connections where appropriate
  - Mark live feed tests with `@Disabled` to avoid external dependencies in CI

Example test structure:
```java
@Test
void shouldHandleNewFeature() {
    // Given
    var request = new RssFeedRequest(/* ... */);
    
    // When
    var result = connector.execute(request);
    
    // Then
    assertThat(result).satisfies(expectedBehavior);
}
```

### Element Template Updates

The element template is auto-generated from Java annotations. When you update the connector's request class:

1. Add `@TemplateProperty` annotations to fields in `RssFeedRequest`

2. Regenerate the template:
   ```bash
   mvn clean package
   ```
3. The template will be updated in `element-templates/rss-feed-connector.json`

### Documentation

When adding features or changing behavior:

- Update `README.md` with new features and examples
- Add entries to `CHANGELOG.md` for user-facing changes
- Update Javadoc comments in code
- Add or update usage examples

## Submission Process

### Reporting Issues

Before creating an issue:

1. Check if the issue already exists
2. Search closed issues for similar problems
3. Ensure you're using the latest version

When creating an issue:

- Use a clear, descriptive title
- Provide steps to reproduce the problem
- Include relevant logs or error messages
- Specify your environment (Java version, Camunda version, etc.)

### Submitting Pull Requests

1. **Fork the repository** and create a feature branch

2. **Make your changes** following the guidelines above

3. **Ensure all tests pass**:
   ```bash
   mvn clean verify
   ```

4. **Update documentation** as needed

5. **Create a Pull Request** with:
   - Clear description of changes
   - Reference to related issues
   - Note any breaking changes
   - Include example usage for new features

## Questions?

If you have questions about contributing:
- Check the [README.md](README.md) for usage and examples
- Review [SECURITY.md](SECURITY.md) for credential setup
- Open an issue for clarification

Thank you for contributing! 🎉

