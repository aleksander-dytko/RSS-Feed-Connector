package io.camunda.connector.rssfeed.dto;

import java.util.List;

/**
 * Represents a single item from an RSS feed.
 * All fields are nullable to gracefully handle incomplete RSS feed items.
 */
public record RssFeedItem(
    /**
     * The title of the RSS item.
     */
    String title,
    
    /**
     * The URL link to the full content.
     */
    String link,
    
    /**
     * The description or summary of the item.
     */
    String description,
    
    /**
     * The publication date in ISO 8601 format (e.g., "2025-10-26T10:30:00Z").
     */
    String publishedDate,
    
    /**
     * The author of the item, if available.
     */
    String author,
    
    /**
     * List of categories or tags associated with this item.
     */
    List<String> categories,
    
    /**
     * The globally unique identifier (GUID) for this item.
     */
    String guid
) {}

