package io.camunda.connector.rssfeed;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.rssfeed.dto.RssFeedItem;
import io.camunda.connector.rssfeed.dto.RssFeedRequest;
import io.camunda.connector.rssfeed.dto.RssFeedResult;
import io.camunda.connector.runtime.test.outbound.OutboundConnectorContextBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for the RSS Feed Connector.
 */
class RssFeedConnectorFunctionTest {

    private final RssFeedConnectorFunction connector = new RssFeedConnectorFunction();

    @Test
    void shouldParseValidRssFeed() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 20, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(15);
        assertThat(result.filteredItems()).isGreaterThan(0);
        assertThat(result.items()).isNotEmpty();
        
        // Verify first item has expected fields
        RssFeedItem firstItem = result.items().get(0);
        assertThat(firstItem.title()).isNotNull();
        assertThat(firstItem.link()).isNotNull();
    }

    @Test
    void shouldHandleEmptyFeed() {
        // Given
        URL feedUrl = getTestResourceUrl("empty-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 10, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(0);
        assertThat(result.filteredItems()).isEqualTo(0);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void shouldThrowErrorForInvalidUrl() {
        // Given
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest("http://not a valid url", 10, null, null))
            .build();

        // When & Then
        assertThatThrownBy(() -> connector.execute(context))
            .isInstanceOf(ConnectorException.class)
            .satisfies(e -> {
                ConnectorException ce = (ConnectorException) e;
                assertThat(ce.getErrorCode()).isEqualTo("INVALID_URL");
            });
    }

    @Test
    void shouldThrowErrorForMalformedXml() {
        // Given
        URL feedUrl = getTestResourceUrl("invalid-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 10, null, null))
            .build();

        // When & Then
        assertThatThrownBy(() -> connector.execute(context))
            .isInstanceOf(ConnectorException.class)
            .satisfies(e -> {
                ConnectorException ce = (ConnectorException) e;
                assertThat(ce.getErrorCode()).isEqualTo("PARSE_ERROR");
            });
    }

    @Test
    void shouldThrowErrorForInvalidDateFormat() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        
        // When & Then - invalid fromDate
        assertThatThrownBy(() -> {
            new RssFeedRequest(feedUrl.toString(), 10, "not-a-date", null).parseFromDate();
        })
            .isInstanceOf(ConnectorException.class)
            .satisfies(e -> {
                ConnectorException ce = (ConnectorException) e;
                assertThat(ce.getErrorCode()).isEqualTo("INVALID_DATE_FORMAT");
            })
            .hasMessageContaining("ISO8601 format");
    }

    @Test
    void shouldParseDateOnlyFormat() {
        // Test FEEL today() format: 2025-10-25
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var request = new RssFeedRequest(feedUrl.toString(), 10, "2025-10-25", null);
        
        // Should not throw and should parse as start of day in UTC
        assertThatCode(() -> request.parseFromDate()).doesNotThrowAnyException();
        assertThat(request.parseFromDate()).isNotNull();
        assertThat(request.parseFromDate().toString()).startsWith("2025-10-25T00:00");
    }

    @Test
    void shouldParseDateTimeWithTimezoneIdentifier() {
        // Test FEEL now() format: 2025-10-25T12:20:31.434Z[GMT]
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var request = new RssFeedRequest(feedUrl.toString(), 10, "2025-10-25T12:20:31.434Z[GMT]", null);
        
        // Should not throw and should parse correctly, ignoring the [GMT] part
        assertThatCode(() -> request.parseFromDate()).doesNotThrowAnyException();
        assertThat(request.parseFromDate()).isNotNull();
        assertThat(request.parseFromDate().toString()).startsWith("2025-10-25T12:20:31");
    }

    @Test
    void shouldParseStandardISO8601DateTime() {
        // Test standard format: 2025-01-01T00:00:00Z
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var request = new RssFeedRequest(feedUrl.toString(), 10, "2025-01-01T00:00:00Z", null);
        
        // Should parse correctly
        assertThatCode(() -> request.parseFromDate()).doesNotThrowAnyException();
        assertThat(request.parseFromDate()).isNotNull();
        assertThat(request.parseFromDate().toString()).startsWith("2025-01-01T00:00");
    }

    @Test
    void shouldFilterUsingFeelDateFormat() {
        // Given - using FEEL date format
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                feedUrl.toString(), 
                50, 
                "2025-10-20",  // Date only format from FEEL today()
                null
            ))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filteredItems()).isLessThan(result.totalItems());
    }

    @Test
    void shouldFilterByFromDate() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        // Filter to only items from October 20, 2025 onwards
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                feedUrl.toString(), 
                50, 
                "2025-10-20T00:00:00Z", 
                null
            ))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filteredItems()).isLessThan(result.totalItems());
        
        // Verify all returned items are from Oct 20 or later
        result.items().forEach(item -> {
            if (item.publishedDate() != null) {
                assertThat(item.publishedDate()).isGreaterThanOrEqualTo("2025-10-20");
            }
        });
    }

    @Test
    void shouldFilterByToDate() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        // Filter to only items up to October 20, 2025
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                feedUrl.toString(), 
                50, 
                null, 
                "2025-10-20T23:59:59Z"
            ))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filteredItems()).isLessThan(result.totalItems());
        
        // Verify all returned items are from Oct 20 or earlier
        result.items().forEach(item -> {
            if (item.publishedDate() != null) {
                assertThat(item.publishedDate()).isLessThanOrEqualTo("2025-10-21");
            }
        });
    }

    @Test
    void shouldFilterByDateRange() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        // Filter to items between Oct 15 and Oct 25, 2025
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                feedUrl.toString(), 
                50, 
                "2025-10-15T00:00:00Z", 
                "2025-10-25T23:59:59Z"
            ))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filteredItems()).isGreaterThan(0);
        assertThat(result.filteredItems()).isLessThan(result.totalItems());
        
        // Verify dates are within range
        result.items().forEach(item -> {
            if (item.publishedDate() != null) {
                assertThat(item.publishedDate()).isBetween("2025-10-15", "2025-10-26");
            }
        });
    }

    @Test
    void shouldLimitMaxItems() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 5, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(15);
        assertThat(result.filteredItems()).isEqualTo(5);
        assertThat(result.items()).hasSize(5);
    }

    @Test
    void shouldUseDefaultMaxItems() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), null, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filteredItems()).isLessThanOrEqualTo(10); // default is 10
    }

    @Test
    void shouldHandleMissingOptionalFields() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 20, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        
        // Find item with minimal fields
        RssFeedItem minimalItem = result.items().stream()
            .filter(item -> "Item With Minimal Fields".equals(item.title()))
            .findFirst()
            .orElse(null);
        
        assertThat(minimalItem).isNotNull();
        assertThat(minimalItem.link()).isNotNull();
        // Description, author, and categories may be null or empty
    }

    @Test
    void shouldSortByPublishedDateDescending() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 10, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.items()).isNotEmpty();
        
        // Verify items are sorted in descending order (most recent first)
        String previousDate = null;
        for (RssFeedItem item : result.items()) {
            if (item.publishedDate() != null) {
                if (previousDate != null) {
                    // Current date should be less than or equal to previous (descending order)
                    assertThat(item.publishedDate()).isLessThanOrEqualTo(previousDate);
                }
                previousDate = item.publishedDate();
            }
        }
    }

    @Test
    void shouldHandleNullPublishedDate() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 20, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        
        // Find item without date
        RssFeedItem noDateItem = result.items().stream()
            .filter(item -> "Item Without Date".equals(item.title()))
            .findFirst()
            .orElse(null);
        
        // Item should be included even without a date (pushed to end)
        assertThat(noDateItem).isNotNull();
        assertThat(noDateItem.publishedDate()).isNull();
    }

    @Test
    void shouldHandleCategoriesCorrectly() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 20, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        RssFeedItem itemWithCategories = result.items().stream()
            .filter(item -> "Latest News Item".equals(item.title()))
            .findFirst()
            .orElse(null);
        
        assertThat(itemWithCategories).isNotNull();
        assertThat(itemWithCategories.categories()).isNotNull();
        assertThat(itemWithCategories.categories()).contains("Technology", "News");
    }

    @Test
    @Disabled("Enable for manual testing with live BBC RSS feed")
    void shouldFetchLiveBbcRssFeed() {
        // Given
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                "https://feeds.bbci.co.uk/news/rss.xml", 
                5, 
                null, 
                null
            ))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.items()).isNotEmpty();
        assertThat(result.filteredItems()).isLessThanOrEqualTo(5);
        
        System.out.println("Fetched " + result.filteredItems() + " items from BBC News RSS feed");
        result.items().forEach(item -> 
            System.out.println("- " + item.title() + " (" + item.publishedDate() + ")")
        );
    }

    @Test
    void shouldIncludeFeedMetadata() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(feedUrl.toString(), 10, null, null))
            .build();

        // When
        RssFeedResult result = (RssFeedResult) connector.execute(context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.metadata()).isNotNull();
        assertThat(result.metadata().title()).isEqualTo("Test RSS Feed");
        assertThat(result.metadata().description()).isEqualTo("A test RSS feed for unit testing");
        assertThat(result.metadata().link()).isEqualTo("https://example.com");
    }

    @Test
    void shouldThrowErrorWhenFromDateAfterToDate() {
        // Given
        URL feedUrl = getTestResourceUrl("test-feed.xml");
        var context = OutboundConnectorContextBuilder.create()
            .variables(new RssFeedRequest(
                feedUrl.toString(),
                10,
                "2025-10-25T00:00:00Z",  // fromDate is AFTER toDate
                "2025-10-20T00:00:00Z"   // toDate
            ))
            .build();

        // When & Then
        assertThatThrownBy(() -> connector.execute(context))
            .isInstanceOf(ConnectorException.class)
            .satisfies(e -> {
                ConnectorException ce = (ConnectorException) e;
                assertThat(ce.getErrorCode()).isEqualTo("INVALID_DATE_RANGE");
            })
            .hasMessageContaining("fromDate must be before or equal to toDate");
    }

    /**
     * Helper method to get a test resource file URL.
     */
    private URL getTestResourceUrl(String filename) {
        URL resourceUrl = getClass().getClassLoader().getResource(filename);
        assertThat(resourceUrl).isNotNull();
        return resourceUrl;
    }
}

