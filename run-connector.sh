#!/bin/bash

# RSS Feed Connector Runner Script
# Based on Camunda Connector SDK documentation

case "$1" in
    "saas")
        echo "🚀 Starting RSS Feed Connector for Camunda SaaS..."
        echo "📝 Using credentials from application-saas.properties"
        mvn exec:java -Dspring.config.additional-location=src/test/resources/application-saas.properties
        ;;
    "local")
        echo "🏠 Starting RSS Feed Connector for Local Development..."
        mvn exec:java -Dspring.config.additional-location=src/test/resources/application-local.properties
        ;;
    "test")
        echo "🧪 Running RSS Feed Connector Tests..."
        mvn clean verify
        ;;
    *)
        echo "Usage: $0 {saas|local|test}"
        echo ""
        echo "Commands:"
        echo "  saas   - Run connector connected to Camunda SaaS"
        echo "  local  - Run connector connected to local Camunda Platform"
        echo "  test   - Run unit tests"
        echo ""
        echo "Examples:"
        echo "  $0 saas    # Connect to SaaS cluster"
        echo "  $0 local   # Connect to local Docker instance"
        echo "  $0 test    # Run tests"
        exit 1
        ;;
esac
