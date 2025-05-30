# Use a lightweight base image
FROM alpine:latest

# Install bash for the script (alpine's sh might have limitations for some scripts)
RUN apk add --no-cache bash

# Create directory for logs
RUN mkdir -p /data/logs

# Copy the logger script into the container
COPY logger.sh /usr/local/bin/logger.sh

# Make the script executable
RUN chmod +x /usr/local/bin/logger.sh

# Command to run when the container starts
CMD ["/usr/local/bin/logger.sh"]
