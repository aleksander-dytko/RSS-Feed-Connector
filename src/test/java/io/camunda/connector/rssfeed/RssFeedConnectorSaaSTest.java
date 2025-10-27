package io.camunda.connector.rssfeed;

import io.camunda.connector.rssfeed.dto.RssFeedRequest;
import io.camunda.connector.rssfeed.dto.RssFeedResult;
import io.camunda.connector.runtime.test.outbound.OutboundConnectorContextBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the RSS Feed Connector with SaaS configuration
 */
class RssFeedConnectorSaaSTest {

    private final RssFeedConnectorFunction connector = new RssFeedConnectorFunction();

    @Test
    void shouldFetchRssFeedFromSaaS() {
        // Given - Use a real RSS feed URL
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
        assertThat(result.totalItems()).isGreaterThan(0);
        assertThat(result.filteredItems()).isEqualTo(5);
        
        // Verify first item has expected fields
        var firstItem = result.items().get(0);
        assertThat(firstItem.title()).isNotNull();
        assertThat(firstItem.link()).isNotNull();
        assertThat(firstItem.publishedDate()).isNotNull();
        
        System.out.println("✅ RSS Feed Connector test successful!");
        System.out.println("📰 Fetched " + result.filteredItems() + " items from " + result.totalItems() + " total");
        System.out.println("🔗 First item: " + firstItem.title());
    }
}
