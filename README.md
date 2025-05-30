# Camel Quarkus File to Kafka Pipeline

This project demonstrates a data pipeline using Apache Camel on Quarkus. It polls a log file, processes each new line, and sends it as a message to a Kafka topic. The setup is containerized using Docker Compose and uses local file system mounts for log data and checkpointing.

## Features

* **Local File System Integration**: Log files and checkpoint data are stored on your local file system, making them easily accessible.
* **Checkpointing**: The Camel application remembers which log lines have already been processed. If restarted, it will only send new, unprocessed lines to Kafka, preventing duplicates. This is achieved using Camel's idempotent consumer pattern with a file-based repository storing hashes of processed lines.

## Components

1.  **File Logger (`file-logger`)**:
    * A shell script that continuously writes timestamped random log messages to a file.
    * This log file is stored in `./pipeline_data/logs/app.log` on your host and mounted into the container.

2.  **Zookeeper (`zookeeper`)**:
    * Required by Kafka.

3.  **Kafka Broker (`kafka`)**:
    * Receives log messages. Messages are sent to the `log-topic`.
    * Kafka data is persisted in `./pipeline_data/kafka_data` on your host.

4.  **Provectus Kafka UI (`kafka-ui`)**:
    * Web UI for Kafka. Accessible at `http://localhost:8088`.

5.  **Camel Quarkus App (`camel-quarkus-app`)**:
    * Polls the log file (e.g., `/data/logs/app.log` inside the container, mapped from `./pipeline_data/logs/app.log` on host).
    * Splits the file into lines.
    * Uses an idempotent consumer with a `FileIdempotentRepository` (data stored in `./pipeline_data/checkpoint/processedLineHashes.dat` on host) to process each line only once.
    * Sends new lines to the `log-topic` on Kafka.
    * Built as a native executable.

## Project Structure

```
.
├── camel-quarkus-app
│   ├── Dockerfile
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/org/acme/FileToKafkaRoute.java  # Camel route with checkpointing
│       │   └── resources/application.properties
├── docker-compose.yml           # Defines services, uses local mounts
├── file-logger
│   ├── Dockerfile
│   └── logger.sh
├── pipeline_data                # YOU NEED TO CREATE THIS DIRECTORY
│   ├── checkpoint               # For checkpoint data (will be created by app if parent exists)
│   ├── kafka_data               # For Kafka's persistent data
│   └── logs                     # For the generated log file
└── README.md
```

## Prerequisites

* Docker
* Docker Compose
* Maven (for Quarkus app if building outside Docker)
* Java JDK (e.g., JDK 17+)

## How to Run

1.  **Create Local Directories:**
    Before the first run, you **MUST** create the `pipeline_data` directory and its subdirectories in the root of your project:
    ```bash
    mkdir -p pipeline_data/logs
    mkdir -p pipeline_data/checkpoint
    mkdir -p pipeline_data/kafka_data
    ```
    The `file-logger` will write to `pipeline_data/logs/app.log`. The Camel app will read from there and store checkpoint data in `pipeline_data/checkpoint`. Kafka will store its data in `pipeline_data/kafka_data`.

2.  **Build and Start Services:**
    Open a terminal in the project root and run:
    ```bash
    docker-compose up --build
    ```

3.  **Verify Operation:**
    * **File Logger**: Check `pipeline_data/logs/app.log` on your host. It should be populated with logs.
    * **Camel Quarkus App**:
        * Observe its logs via `docker-compose logs -f camel-quarkus-app`.
        * You should see messages about processing new lines and sending them to Kafka.
        * Check `pipeline_data/checkpoint/processedLineHashes.dat` on your host. This file will store hashes of processed lines.
    * **Kafka UI**:
        * Open `http://localhost:8088`.
        * Find `log-topic` under Topics and view its messages.

4.  **Test Checkpointing:**
    * Let the system run for a bit to send some messages.
    * Stop the services: `Ctrl+C` in the `docker-compose` terminal, then `docker-compose down`.
    * Do **not** delete the `pipeline_data/checkpoint` or `pipeline_data/logs` directories.
    * Restart the services: `docker-compose up --build` (or just `docker-compose up` if no code changes).
    * The `file-logger` will continue adding to `app.log`.
    * The `camel-quarkus-app` will re-read `app.log` but should only send lines to Kafka that were added *after* the previous shutdown (i.e., lines whose hashes are not in `processedLineHashes.dat`). You should not see old messages being re-sent to Kafka.

5.  **Stop Services:**
    `Ctrl+C`, then `docker-compose down`. To also remove Kafka's data (if you want a clean slate for Kafka itself, but keep logs and checkpoint data for testing):
    ```bash
    docker-compose down
    # Optionally, to clear Kafka's own data:
    # rm -rf ./pipeline_data/kafka_data/*
    ```

## Configuration

* **Log File (Host)**: `./pipeline_data/logs/app.log`
* **Checkpoint Data (Host)**: `./pipeline_data/checkpoint/processedLineHashes.dat`
* **Kafka Data (Host)**: `./pipeline_data/kafka_data/`
* **Kafka Topic**: `log-topic`
