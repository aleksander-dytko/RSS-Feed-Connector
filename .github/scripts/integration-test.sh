#!/bin/bash
set -euo pipefail

echo "=== RSS Feed Connector Integration Test ==="

# Configuration
ORCHESTRATE_URL="http://localhost:8088"
BPMN_FILE="src/main/resources/fetch_rss_example.bpmn"
PROCESS_ID="fetch_rss"
MAX_WAIT_TIME=60  # seconds
POLL_INTERVAL=2   # seconds

# Step 1: Deploy BPMN process
echo "📦 Deploying BPMN process definition..."
echo "Using BPMN file: ${BPMN_FILE}"
ls -lh "${BPMN_FILE}"

DEPLOYMENT_RESPONSE=$(curl -s -X POST "${ORCHESTRATE_URL}/v2/deployments" \
  -H "Content-Type: multipart/form-data" \
  -H "Accept: application/json" \
  -F "resources=@${BPMN_FILE}")

echo "Deployment response: $DEPLOYMENT_RESPONSE"

# Extract deployment key from JSON
DEPLOYMENT_KEY=$(echo "$DEPLOYMENT_RESPONSE" | grep -o '"deploymentKey":"[0-9]*"' | cut -d'"' -f4)
if [ -z "$DEPLOYMENT_KEY" ]; then
  echo "❌ Failed to deploy BPMN process"
  echo "Full response: $DEPLOYMENT_RESPONSE"
  exit 1
fi
echo "✅ Process deployed successfully (key: ${DEPLOYMENT_KEY})"

# Step 2: Create process instance
echo "🚀 Creating process instance..."
INSTANCE_RESPONSE=$(curl -s -X POST "${ORCHESTRATE_URL}/v2/process-instances" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{\"processDefinitionId\":\"${PROCESS_ID}\",\"variables\":{}}")

echo "Instance creation response: $INSTANCE_RESPONSE"

PROCESS_INSTANCE_KEY=$(echo "$INSTANCE_RESPONSE" | grep -o '"processInstanceKey":"[0-9]*"' | cut -d'"' -f4)
if [ -z "$PROCESS_INSTANCE_KEY" ]; then
  echo "❌ Failed to create process instance"
  echo "Response: $INSTANCE_RESPONSE"
  exit 1
fi
echo "✅ Process instance created (key: ${PROCESS_INSTANCE_KEY})"

# Step 3: Poll for completion
echo "⏳ Waiting for process to complete (max ${MAX_WAIT_TIME}s)..."
ELAPSED=0
STATE=""
while [ $ELAPSED -lt $MAX_WAIT_TIME ]; do
  STATUS_RESPONSE=$(curl -s "${ORCHESTRATE_URL}/v2/process-instances/${PROCESS_INSTANCE_KEY}")
  STATE=$(echo "$STATUS_RESPONSE" | grep -o '"state":"[^"]*"' | cut -d'"' -f4)
  
  if [ "$STATE" == "COMPLETED" ]; then
    echo "✅ Process completed successfully!"
    break
  elif [ "$STATE" == "FAILED" ] || [ "$STATE" == "TERMINATED" ]; then
    echo "❌ Process failed or was terminated (state: ${STATE})"
    echo "Response: $STATUS_RESPONSE"
    docker logs rssfeed-connector
    exit 1
  fi
  
  echo "  State: ${STATE} (${ELAPSED}s elapsed)"
  sleep $POLL_INTERVAL
  ELAPSED=$((ELAPSED + POLL_INTERVAL))
done

if [ "$STATE" != "COMPLETED" ]; then
  echo "❌ Process did not complete within ${MAX_WAIT_TIME} seconds (final state: ${STATE})"
  docker logs rssfeed-connector
  exit 1
fi

# Step 4: Verify connector logs
echo "🔍 Checking connector logs..."
if docker logs rssfeed-connector 2>&1 | grep -q "Parsed.*items, filtered to.*items"; then
  echo "✅ Connector executed and processed RSS feed items"
else
  echo "⚠️  Warning: Could not find expected log message about items being parsed"
  docker logs rssfeed-connector
fi

echo ""
echo "✅ Integration test passed successfully!"

