package io.camunda.connector.rssfeed.dto;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Request object for the RSS Feed Connector.
 * Contains the feed URL and optional filtering parameters.
 */
public record RssFeedRequest(
    /**
     * The URL of the RSS feed to fetch.
     * Must be a valid HTTP or HTTPS URL.
     * Example: https://feeds.bbci.co.uk/news/rss.xml
     */
    @NotBlank(message = "Feed URL is required")
    @TemplateProperty(
        group = "configuration",
        label = "Feed URL",
        description = "The URL of the RSS feed to fetch (e.g., https://feeds.bbci.co.uk/news/rss.xml)",
        type = PropertyType.String
    )
    String feedUrl,
    
    /**
     * Maximum number of items to return (after filtering).
     * Must be between 1 and 500. Defaults to 10.
     */
    @Min(value = 1, message = "Max items must be at least 1")
    @Max(value = 500, message = "Max items cannot exceed 500")
    @TemplateProperty(
        group = "configuration",
        label = "Max Items",
        description = "Maximum number of items to return (default: 10)",
        defaultValue = "10",
        type = PropertyType.String
    )
    Integer maxItems,
    
    /**
     * Filter items published on or after this date.
     * Must be in ISO 8601 format (e.g., "2025-01-01T00:00:00Z").
     * Supports FEEL expressions like today() or now().
     */
    @TemplateProperty(
        group = "configuration",
        label = "From Date",
        description = "Filter items published on or after this date (ISO8601 format, e.g., 2025-01-01T00:00:00Z or FEEL: today())",
        optional = true,
        type = PropertyType.String
    )
    String fromDate,
    
    /**
     * Filter items published on or before this date.
     * Must be in ISO 8601 format (e.g., "2025-12-31T23:59:59Z").
     * Supports FEEL expressions like today() or now().
     */
    @TemplateProperty(
        group = "configuration",
        label = "To Date",
        description = "Filter items published on or before this date (ISO8601 format, e.g., 2025-12-31T23:59:59Z or FEEL: today())",
        optional = true,
        type = PropertyType.String
    )
    String toDate
) {
    private static final int DEFAULT_MAX_ITEMS = 10;

    /**
     * Get the maximum number of items, with a default of 10 if not specified.
     */
    public int getMaxItemsOrDefault() {
        return maxItems != null ? maxItems : DEFAULT_MAX_ITEMS;
    }
    
    /**
     * Parse the fromDate string into an OffsetDateTime.
     * 
     * @return the parsed date, or null if fromDate is not set
     * @throws ConnectorException if the date format is invalid
     */
    public OffsetDateTime parseFromDate() {
        return parseDate(fromDate, "fromDate");
    }
    
    /**
     * Parse the toDate string into an OffsetDateTime.
     * 
     * @return the parsed date, or null if toDate is not set
     * @throws ConnectorException if the date format is invalid
     */
    public OffsetDateTime parseToDate() {
        return parseDate(toDate, "toDate");
    }
    
    /**
     * Parse a date string into an OffsetDateTime using ISO 8601 format.
     * Supports multiple formats including those returned by FEEL functions:
     * - Date only (from today()): 2025-10-25
     * - DateTime with Z: 2025-01-01T00:00:00Z
     * - DateTime with timezone identifier: 2025-10-25T12:20:31.434Z[GMT]
     * 
     * @param dateString the date string to parse
     * @param fieldName the name of the field (for error messages)
     * @return the parsed date, or null if dateString is null or empty
     * @throws ConnectorException if the date format is invalid
     */
    private OffsetDateTime parseDate(String dateString, String fieldName) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        String normalizedDate = dateString.trim();
        
        // Handle FEEL datetime with timezone identifier like "2025-10-25T12:20:31.434Z[GMT]"
        // Remove the timezone identifier in brackets
        if (normalizedDate.contains("[")) {
            normalizedDate = normalizedDate.substring(0, normalizedDate.indexOf('['));
        }
        
        try {
            // Try to parse as OffsetDateTime (with time component)
            return OffsetDateTime.parse(normalizedDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // Try to parse as LocalDate (date only, from FEEL today() function)
                // Convert to OffsetDateTime at start of day in UTC
                LocalDate date = LocalDate.parse(normalizedDate, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            } catch (DateTimeParseException e2) {
                throw new ConnectorException(
                    "INVALID_DATE_FORMAT",
                    fieldName + " must follow ISO8601 format. Supported formats: " +
                    "date (e.g., 2025-01-01), datetime (e.g., 2025-01-01T00:00:00Z). Received: " + dateString,
                    e2
                );
            }
        }
    }
}

