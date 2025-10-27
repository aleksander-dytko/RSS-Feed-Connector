#!/bin/bash

# RSS Feed Connector Setup Script
# This script helps you set up secure credential management

set -e

echo "🔐 RSS Feed Connector - Secure Setup"
echo "===================================="
echo ""

# Check if application-local.properties already exists
if [ -f "src/test/resources/application-local.properties" ]; then
    echo "✅ application-local.properties already exists"
else
    echo "📝 Creating application-local.properties for local Docker..."
    cat > src/test/resources/application-local.properties << 'EOF'
# RSS Feed Connector Configuration - LOCAL ONLY
# This file contains local development settings and should NEVER be committed to git

# Server configuration
server.port=9898

# Camunda Local Docker Configuration
camunda.client.mode=local
camunda.client.security.plaintext=true
camunda.client.broker.gateway-address=localhost:26500

# Operate Configuration (for local development)
camunda.operate.url=http://localhost:8088
camunda.operate.username=demo
camunda.operate.password=demo

# Connector runtime configuration
camunda.connector.polling.enabled=true
camunda.connector.polling.max-jobs-active=10

# Logging configuration
logging.level.io.camunda.connector=INFO
logging.level.io.camunda.zeebe=INFO
logging.level.io.camunda.connector.rssfeed=DEBUG
logging.level.root=WARN
EOF
    echo "✅ Created application-local.properties"
fi

# Check if application-saas.properties already exists
if [ -f "src/test/resources/application-saas.properties" ]; then
    echo "✅ application-saas.properties already exists"
else
    echo "📝 Creating application-saas.properties from template..."
    cp src/test/resources/application.properties.template src/test/resources/application-saas.properties
    echo "✅ Created application-saas.properties"
    echo ""
    echo "⚠️  Important: Edit src/test/resources/application-saas.properties with your SaaS credentials!"
fi

echo ""
echo "🔧 Next Steps:"
echo "1. Local Docker: application-local.properties is ready (no changes needed)"
echo "2. SaaS: Edit src/test/resources/application-saas.properties with your actual SaaS credentials"
echo "3. Run the connector:"
echo "   ./run-connector.sh local  # For local Docker"
echo "   ./run-connector.sh saas   # For SaaS"
echo ""
echo "⚠️  Security Notes:"
echo "- application-saas.properties is in .gitignore (contains sensitive credentials)"
echo "- application-local.properties is safe to commit (non-sensitive local dev settings)"
echo "- Never commit credentials to git"
echo "- Use environment variables in production"
echo ""
echo "🎉 Setup complete!"
