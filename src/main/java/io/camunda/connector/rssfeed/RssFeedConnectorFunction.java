package io.camunda.connector.rssfeed;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.rssfeed.dto.FeedMetadata;
import io.camunda.connector.rssfeed.dto.RssFeedItem;
import io.camunda.connector.rssfeed.dto.RssFeedRequest;
import io.camunda.connector.rssfeed.dto.RssFeedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RSS Feed Connector for Camunda 8.
 * Fetches and parses RSS feed data from a specified URL with optional
 * filtering.
 */
@OutboundConnector(name = "RssFeedConnector", inputVariables = { "feedUrl", "maxItems", "fromDate",
        "toDate" }, type = "io.camunda:rssfeed:1")
@ElementTemplate(id = "io.camunda.connector.rssfeed.v1", name = "RSS Feed Connector", version = 1, description = "Fetches and parses RSS feed data from a specified URL", icon = "icon.svg", documentationRef = "https://github.com/aleksander-dytko/RSS-Feed-Connector", propertyGroups = {
        @ElementTemplate.PropertyGroup(id = "configuration", label = "Configuration")
}, inputDataClass = RssFeedRequest.class)
public class RssFeedConnectorFunction implements OutboundConnectorFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RssFeedConnectorFunction.class);

    // Configuration constants
    private static final int SAFETY_LIMIT_ITEMS = 500;
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public RssFeedConnectorFunction() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // Constructor for testing with custom HttpClient
    RssFeedConnectorFunction(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Object execute(OutboundConnectorContext context) {
        final var connectorRequest = context.bindVariables(RssFeedRequest.class);

        // Enhanced logging with process context
        Long processInstanceKey = null;
        try {
            processInstanceKey = context.getJobContext().getProcessInstanceKey();
        } catch (Exception e) {
            // Context might not have process instance key in all environments
            LOGGER.debug("Could not retrieve process instance key", e);
        }

        LOGGER.info(
                "Executing RSS Feed Connector [processInstanceKey={}] with URL: {}, maxItems: {}, fromDate: {}, toDate: {}",
                processInstanceKey,
                connectorRequest.feedUrl(),
                connectorRequest.getMaxItemsOrDefault(),
                connectorRequest.fromDate(),
                connectorRequest.toDate());

        return executeConnector(connectorRequest);
    }

    /**
     * Main connector execution logic.
     * 
     * @param request the validated request containing feed URL and filter
     *                parameters
     * @return the result containing filtered feed items
     * @throws ConnectorException if any error occurs during fetching or parsing
     */
    private RssFeedResult executeConnector(final RssFeedRequest request) {
        // Validate and parse URL
        URI feedUri = validateAndParseUrl(request.feedUrl());

        // Parse optional date filters and validate date range
        OffsetDateTime fromDate = request.parseFromDate();
        OffsetDateTime toDate = request.parseToDate();

        // Cross-field validation: fromDate must be before toDate
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ConnectorException(
                    "INVALID_DATE_RANGE",
                    "fromDate must be before or equal to toDate. Received fromDate: " +
                            request.fromDate() + ", toDate: " + request.toDate());
        }

        // Fetch and parse the RSS feed
        SyndFeed feed = fetchFeed(feedUri);

        // Get all entries (limit in-memory for safety)
        int originalSize = feed.getEntries().size();
        List<SyndEntry> entries = feed.getEntries().stream()
                .limit(SAFETY_LIMIT_ITEMS)
                .collect(Collectors.toList());

        int totalItems = entries.size();

        // Warn if truncation occurred
        if (originalSize > SAFETY_LIMIT_ITEMS) {
            LOGGER.warn("Feed contains {} items, but only {} items will be processed due to safety limit. " +
                    "Consider filtering at the source or adjusting SAFETY_LIMIT_ITEMS.",
                    originalSize, SAFETY_LIMIT_ITEMS);
        }

        LOGGER.debug("Fetched {} items from feed: {}", totalItems, feed.getTitle());

        // Convert to our DTO objects and apply filtering
        List<RssFeedItem> items = entries.stream()
                .map(this::convertToRssFeedItem)
                .filter(item -> matchesDateFilter(item, fromDate, toDate))
                .sorted(Comparator.comparing(
                        RssFeedItem::publishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(request.getMaxItemsOrDefault())
                .collect(Collectors.toList());

        int filteredItems = items.size();
        LOGGER.info("Parsed {} items, filtered to {} items", totalItems, filteredItems);

        // Extract feed metadata
        FeedMetadata metadata = extractFeedMetadata(feed);

        return new RssFeedResult(items, totalItems, filteredItems, metadata);
    }

    /**
     * Validate and parse the feed URL.
     * 
     * @param urlString the URL string to validate
     * @return the parsed URI
     * @throws ConnectorException if the URL is malformed
     */
    private URI validateAndParseUrl(String urlString) {
        try {
            URI uri = URI.create(urlString);
            // Validate scheme is HTTP, HTTPS, or file (file for testing)
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") &&
                    !scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("file"))) {
                throw new ConnectorException(
                        "INVALID_URL",
                        "URL must use HTTP, HTTPS, or file scheme. Received: " + urlString);
            }
            return uri;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid URL: {}", urlString, e);
            throw new ConnectorException(
                    "INVALID_URL",
                    "The provided URL is malformed: " + urlString,
                    e);
        }
    }

    /**
     * Fetch and parse the RSS feed from the given URI using HttpClient.
     *
     * @param uri the feed URI
     * @return the parsed feed
     * @throws ConnectorException if fetching or parsing fails
     */
    private SyndFeed fetchFeed(URI uri) {
        // Handle file:// URLs for testing purposes
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return fetchFeedFromFile(uri);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(HTTP_REQUEST_TIMEOUT)
                    .header("User-Agent", "Camunda-RSS-Feed-Connector/1.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            // Check for successful response
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ConnectorException(
                        "FETCH_ERROR",
                        "Failed to fetch RSS feed. HTTP status code: " + statusCode);
            }

            try (InputStream inputStream = response.body();
                    XmlReader reader = new XmlReader(inputStream)) {
                SyndFeedInput input = new SyndFeedInput();
                return input.build(reader);
            }
        } catch (FeedException e) {
            LOGGER.error("Failed to parse RSS feed from URI: {}", uri, e);
            throw new ConnectorException(
                    "PARSE_ERROR",
                    "Failed to parse RSS feed. The content may not be valid RSS/Atom XML: " + e.getMessage(),
                    e);
        } catch (IOException e) {
            LOGGER.error("Failed to fetch RSS feed from URI: {}", uri, e);
            throw new ConnectorException(
                    "FETCH_ERROR",
                    "Failed to fetch RSS feed from URI. Network or server error: " + e.getMessage(),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Request interrupted while fetching RSS feed from URI: {}", uri, e);
            throw new ConnectorException(
                    "FETCH_ERROR",
                    "Request was interrupted while fetching RSS feed: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Fetch and parse RSS feed from a file URI (for testing purposes).
     *
     * @param uri the file URI
     * @return the parsed feed
     * @throws ConnectorException if fetching or parsing fails
     */
    private SyndFeed fetchFeedFromFile(URI uri) {
        try (InputStream inputStream = uri.toURL().openStream();
                XmlReader reader = new XmlReader(inputStream)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(reader);
        } catch (FeedException e) {
            LOGGER.error("Failed to parse RSS feed from file URI: {}", uri, e);
            throw new ConnectorException(
                    "PARSE_ERROR",
                    "Failed to parse RSS feed. The content may not be valid RSS/Atom XML: " + e.getMessage(),
                    e);
        } catch (IOException e) {
            LOGGER.error("Failed to read RSS feed from file URI: {}", uri, e);
            throw new ConnectorException(
                    "FETCH_ERROR",
                    "Failed to read RSS feed from file: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Convert a Rome SyndEntry to our RssFeedItem DTO.
     * Gracefully handles missing fields by setting them to null or empty lists.
     * 
     * @param entry the syndication entry
     * @return the converted feed item
     */
    private RssFeedItem convertToRssFeedItem(SyndEntry entry) {
        String publishedDate = null;
        if (entry.getPublishedDate() != null) {
            publishedDate = formatDate(entry.getPublishedDate());
        } else if (entry.getUpdatedDate() != null) {
            publishedDate = formatDate(entry.getUpdatedDate());
        }

        List<String> categories = entry.getCategories() != null
                ? entry.getCategories().stream()
                        .filter(Objects::nonNull)
                        .map(cat -> cat.getName())
                        .filter(name -> name != null && !name.isEmpty())
                        .collect(Collectors.toList())
                : List.of();

        String description = null;
        if (entry.getDescription() != null) {
            description = entry.getDescription().getValue();
        }

        return new RssFeedItem(
                entry.getTitle(),
                entry.getLink(),
                description,
                publishedDate,
                entry.getAuthor(),
                categories,
                entry.getUri());
    }

    /**
     * Extract metadata from the feed.
     *
     * @param feed the syndication feed
     * @return the feed metadata
     */
    private FeedMetadata extractFeedMetadata(SyndFeed feed) {
        String lastBuildDate = null;
        if (feed.getPublishedDate() != null) {
            lastBuildDate = formatDate(feed.getPublishedDate());
        }

        return new FeedMetadata(
                feed.getTitle(),
                feed.getDescription(),
                feed.getLink(),
                lastBuildDate);
    }

    /**
     * Format a Date to ISO 8601 string.
     * 
     * @param date the date to format
     * @return the formatted date string
     */
    private String formatDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Check if an item matches the date filter criteria.
     * 
     * @param item     the feed item
     * @param fromDate the minimum date (inclusive), or null for no minimum
     * @param toDate   the maximum date (inclusive), or null for no maximum
     * @return true if the item matches the filter
     */
    private boolean matchesDateFilter(RssFeedItem item, OffsetDateTime fromDate, OffsetDateTime toDate) {
        // If no date filters, all items match
        if (fromDate == null && toDate == null) {
            return true;
        }

        // If item has no published date, include it (can be filtered by user later)
        if (item.publishedDate() == null) {
            return true;
        }

        try {
            OffsetDateTime itemDate = OffsetDateTime.parse(item.publishedDate(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // Check from date (inclusive)
            if (fromDate != null && itemDate.isBefore(fromDate)) {
                return false;
            }

            // Check to date (inclusive)
            if (toDate != null && itemDate.isAfter(toDate)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            // If we can't parse the date, include the item
            LOGGER.debug("Could not parse date for filtering: {}", item.publishedDate());
            return true;
        }
    }
}
