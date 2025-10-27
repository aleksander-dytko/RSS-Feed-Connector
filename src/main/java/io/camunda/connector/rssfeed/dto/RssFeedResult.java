package io.camunda.connector.rssfeed.dto;

import java.util.List;

/**
 * Result object containing the fetched and filtered RSS feed items.
 */
public record RssFeedResult(
    /**
     * List of RSS feed items after filtering and limiting.
     */
    List<RssFeedItem> items,
    
    /**
     * Total number of items in the original feed before any filtering.
     */
    int totalItems,
    
    /**
     * Number of items after applying filters and limits.
     */
    int filteredItems,

    /**
     * Metadata about the RSS feed itself (title, description, link, etc.).
     */
    FeedMetadata metadata
) {}

