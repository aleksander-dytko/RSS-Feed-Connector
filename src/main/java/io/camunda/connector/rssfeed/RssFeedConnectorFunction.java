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
import io.camunda.connector.rssfeed.dto.RssFeedItem;
import io.camunda.connector.rssfeed.dto.RssFeedRequest;
import io.camunda.connector.rssfeed.dto.RssFeedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RSS Feed Connector for Camunda 8.
 * Fetches and parses RSS feed data from a specified URL with optional filtering.
 */
@OutboundConnector(
    name = "RssFeedConnector",
    inputVariables = {"feedUrl", "maxItems", "fromDate", "toDate"},
    type = "io.camunda:rssfeed:1"
)
@ElementTemplate(
    id = "io.camunda.connector.rssfeed.v1",
    name = "RSS Feed Connector",
    version = 1,
    description = "Fetches and parses RSS feed data from a specified URL",
    icon = "icon.svg",
    documentationRef = "https://github.com/aleksander-dytko/RSS-Feed-Connector",
    propertyGroups = {
        @ElementTemplate.PropertyGroup(id = "configuration", label = "Configuration")
    },
    inputDataClass = RssFeedRequest.class
)
public class RssFeedConnectorFunction implements OutboundConnectorFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RssFeedConnectorFunction.class);
    
    @Override
    public Object execute(OutboundConnectorContext context) {
        final var connectorRequest = context.bindVariables(RssFeedRequest.class);
        LOGGER.info("Executing RSS Feed Connector with URL: {}, maxItems: {}, fromDate: {}, toDate: {}",
            connectorRequest.feedUrl(),
            connectorRequest.getMaxItemsOrDefault(),
            connectorRequest.fromDate(),
            connectorRequest.toDate()
        );
        return executeConnector(connectorRequest);
    }

    /**
     * Main connector execution logic.
     * 
     * @param request the validated request containing feed URL and filter parameters
     * @return the result containing filtered feed items
     * @throws ConnectorException if any error occurs during fetching or parsing
     */
    private RssFeedResult executeConnector(final RssFeedRequest request) {
        // Validate and parse URL
        URL feedUrl = validateAndParseUrl(request.feedUrl());
        
        // Parse optional date filters
        OffsetDateTime fromDate = request.parseFromDate();
        OffsetDateTime toDate = request.parseToDate();
        
        // Fetch and parse the RSS feed
        SyndFeed feed = fetchFeed(feedUrl);
        
        // Get all entries (limit in-memory to 500 for safety)
        List<SyndEntry> entries = feed.getEntries().stream()
            .limit(500)
            .collect(Collectors.toList());
        
        int totalItems = entries.size();
        LOGGER.debug("Fetched {} items from feed: {}", totalItems, feed.getTitle());
        
        // Convert to our DTO objects and apply filtering
        List<RssFeedItem> items = entries.stream()
            .map(this::convertToRssFeedItem)
            .filter(item -> matchesDateFilter(item, fromDate, toDate))
            .sorted(Comparator.comparing(
                RssFeedItem::publishedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .limit(request.getMaxItemsOrDefault())
            .collect(Collectors.toList());
        
        int filteredItems = items.size();
        LOGGER.info("Parsed {} items, filtered to {} items", totalItems, filteredItems);
        
        return new RssFeedResult(items, totalItems, filteredItems);
    }
    
    /**
     * Validate and parse the feed URL.
     * 
     * @param urlString the URL string to validate
     * @return the parsed URL
     * @throws ConnectorException if the URL is malformed
     */
    private URL validateAndParseUrl(String urlString) {
        try {
            URI uri = URI.create(urlString);
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            LOGGER.error("Invalid URL: {}", urlString, e);
            throw new ConnectorException(
                "INVALID_URL",
                "The provided URL is malformed: " + urlString,
                e
            );
        }
    }
    
    /**
     * Fetch and parse the RSS feed from the given URL.
     * 
     * @param url the feed URL
     * @return the parsed feed
     * @throws ConnectorException if fetching or parsing fails
     */
    private SyndFeed fetchFeed(URL url) {
        try (InputStream inputStream = url.openStream();
             XmlReader reader = new XmlReader(inputStream)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(reader);
        } catch (FeedException e) {
            LOGGER.error("Failed to parse RSS feed from URL: {}", url, e);
            throw new ConnectorException(
                "PARSE_ERROR",
                "Failed to parse RSS feed. The content may not be valid RSS/Atom XML: " + e.getMessage(),
                e
            );
        } catch (IOException e) {
            LOGGER.error("Failed to fetch RSS feed from URL: {}", url, e);
            throw new ConnectorException(
                "FETCH_ERROR",
                "Failed to fetch RSS feed from URL. Network or server error: " + e.getMessage(),
                e
            );
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
            entry.getUri()
        );
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
     * @param item the feed item
     * @param fromDate the minimum date (inclusive), or null for no minimum
     * @param toDate the maximum date (inclusive), or null for no maximum
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
            OffsetDateTime itemDate = OffsetDateTime.parse(item.publishedDate(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
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

