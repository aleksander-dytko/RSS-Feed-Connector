package io.camunda.connector.rssfeed.dto;

/**
 * Metadata about the RSS feed itself.
 * All fields are nullable to gracefully handle incomplete feed metadata.
 */
public record FeedMetadata(
    /**
     * The title of the RSS feed.
     */
    String title,

    /**
     * The description of the RSS feed.
     */
    String description,

    /**
     * The link to the feed's website.
     */
    String link,

    /**
     * The last build/publication date of the feed in ISO 8601 format.
     */
    String lastBuildDate
) {}

