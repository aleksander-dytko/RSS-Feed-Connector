# Security Best Practices

## 🔐 Credential Management

This project implements secure credential management to prevent sensitive data from being committed to version control.

### Files Structure

- `application.properties` - Main configuration with environment variable placeholders
- `application.properties.template` - Template file with placeholder values
- `application-local.properties` - Local Docker development settings (non-sensitive, can be committed)
- `application-saas.properties` - **LOCAL ONLY** - Contains your actual SaaS credentials (gitignored)
- `.gitignore` - Prevents sensitive files from being committed

### Setup Instructions

1. **Run the setup script:**
   ```bash
   ./setup-credentials.sh
   ```

2. **Edit your local credentials:**
   ```bash
   # For local Docker development (application-local.properties is ready)
   # For SaaS, edit the SaaS properties file
   nano src/test/resources/application-saas.properties
   ```

3. **Alternative: Use environment variables:**
   ```bash
   export CAMUNDA_CLIENT_ID='your-client-id'
   export CAMUNDA_CLIENT_SECRET='your-client-secret'
   export CAMUNDA_CLUSTER_ID='your-cluster-id'
   export CAMUNDA_CLOUD_REGION='your-region'
   ```

### Running the Connector

```bash
# Local Docker with docker-compose
./run-connector.sh local

# SaaS with your credentials
./run-connector.sh saas

# Run tests
./run-connector.sh test
```

### Security Notes

- ✅ `application-saas.properties` is in `.gitignore` (contains sensitive credentials)
- ✅ `application-local.properties` is safe to commit (contains only non-sensitive local dev settings)
- ✅ Main `application.properties` uses environment variable placeholders
- ✅ Template file provides setup guidance
- ⚠️ Never commit credentials to git
- ⚠️ Use environment variables in production
- ⚠️ Rotate credentials regularly

### Production Deployment

For production environments, use environment variables:

```bash
docker run --rm --name=CustomConnectorInSaaS \
    -v $PWD/target/connector-rssfeed-0.1.0-SNAPSHOT.jar:/opt/app/connector.jar \
    -e CAMUNDA_CLIENT_SECURITY_PLAINTEXT=false \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_ID='$CAMUNDA_CLIENT_ID' \
    -e CAMUNDA_CLIENT_AUTH_CLIENT_SECRET='$CAMUNDA_CLIENT_SECRET' \
    -e CAMUNDA_CLIENT_CLOUD_CLUSTER_ID='$CAMUNDA_CLUSTER_ID' \
    -e CAMUNDA_CLIENT_CLOUD_REGION='$CAMUNDA_CLOUD_REGION' \
    -e CAMUNDA_OPERATE_CLIENT_URL='https://$CAMUNDA_CLOUD_REGION.operate.camunda.io/$CAMUNDA_CLUSTER_ID' \
    camunda/connectors-bundle:8.8.1
```
