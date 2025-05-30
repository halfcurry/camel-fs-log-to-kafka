#!/bin/bash

LOG_FILE="/data/logs/app.log"
LOG_DIR=$(dirname "$LOG_FILE")

# Ensure log directory exists
mkdir -p "$LOG_DIR"

echo "File logger started. Writing to $LOG_FILE"

# Continuously append logs
while true; do
  TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S.%3N")
  RANDOM_SUFFIX=$((RANDOM % 900 + 100)) # Random 3-digit number
  LOG_LEVELS=("INFO" "WARNING" "ERROR" "DEBUG")
  RANDOM_LEVEL=${LOG_LEVELS[$((RANDOM % ${#LOG_LEVELS[@]}))]} # Pick a random log level

  MESSAGE="[$TIMESTAMP] [$RANDOM_LEVEL] This is a random log message. ID: ${RANDOM_SUFFIX}."

  echo "$MESSAGE" >> "$LOG_FILE"
  echo "Logged: $MESSAGE" # Also print to stdout for docker-compose logs

  # Wait for a random interval between 1 and 3 seconds
  sleep $((RANDOM % 3 + 1))
done
