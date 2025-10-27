package io.camunda.connector.rssfeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Local Connector Runtime for RSS Feed Connector
 * Based on Camunda Connector SDK documentation
 * https://docs.camunda.io/docs/components/connectors/custom-built-connectors/connector-sdk/
 */
@SpringBootApplication
public class RssFeedConnectorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RssFeedConnectorApplication.class, args);
    }
}
