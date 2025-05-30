# Camel Quarkus File to Kafka Pipeline

This project demonstrates a simple data pipeline using Apache Camel on Quarkus. It tails a log file, processes each line, and sends it as a message to a Kafka topic. The entire setup is containerized using Docker Compose.

## Components

1.  **File Logger (`file-logger`)**:
    * A simple shell script that continuously writes timestamped random log messages to `/data/logs/app.log`.
    * This log file is shared with the Camel Quarkus app via a Docker volume.

2.  **Zookeeper (`zookeeper`)**:
    * Required by Kafka for cluster management.

3.  **Kafka Broker (`kafka`)**:
    * The message broker that receives log messages from the Camel application.
    * Messages are sent to the `log-topic` topic.

4.  **Provectus Kafka UI (`kafka-ui`)**:
    * A web-based UI to view Kafka topics, messages, consumer groups, etc.
    * Accessible at `http://localhost:8088`.

5.  **Camel Quarkus App (`camel-quarkus-app`)**:
    * A Quarkus application with an Apache Camel route.
    * It uses the `camel-stream` component to tail `/data/logs/app.log`.
    * Each log line is converted to a String and sent to the `log-topic` on the Kafka broker.
    * Built as a native executable for a small footprint and fast startup.

## Project Structure

```
.
├── camel-quarkus-app
│   ├── Dockerfile               # For building the native Quarkus app
│   ├── pom.xml                  # Maven project configuration
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── org
│       │   │       └── acme
│       │   │           └── FileToKafkaRoute.java  # Camel route definition
│       │   └── resources
│       │       └── application.properties       # Quarkus configuration
├── docker-compose.yml           # Docker Compose file to orchestrate services
├── file-logger
│   ├── Dockerfile               # For building the file-logger service
│   └── logger.sh                # Script to generate log messages
└── README.md                    # This file
```


## Prerequisites

* Docker
* Docker Compose
* Maven (if you want to build the Quarkus app outside of Docker, though the provided `Dockerfile` handles this)
* Java JDK (corresponding to the one used in `pom.xml`, e.g., JDK 17+)

## How to Run

1.  **Clone the repository (or create the files as provided).**

2.  **Build and Start Services:**
    Open a terminal in the root directory of the project and run:
    ```bash
    docker-compose up --build
    ```
    This command will:
    * Build the `file-logger` image.
    * Build the `camel-quarkus-app` native executable image (this might take a few minutes the first time).
    * Start all defined services.

3.  **Verify Operation:**
    * **File Logger**: You should see logs from the `file-logger` service in the `docker-compose` output, indicating it's writing to the log file.
    * **Camel Quarkus App**: Logs from this service will show it connecting to Kafka and processing lines from the file. Example: `INFO  [org.acm.FilToKafRou] (Camel (camel-1) thread #2 - stream://file) Read from file: [timestamp] Random log message XYZ`
    * **Kafka UI**:
        * Open your web browser and navigate to `http://localhost:8088`.
        * You should see the `local-kafka` cluster.
        * Navigate to the `Topics` section. You should find `log-topic`.
        * Click on `log-topic` and go to the `Messages` tab to see the log entries being streamed from the file.

4.  **Inspect Shared Log File (Optional):**
    The log file is stored in a Docker volume named `shared_logs_volume`. You can inspect the volume or, if you modify `docker-compose.yml` to map it to a host directory, you can view it directly on your host.

    To see where Docker stores the volume (example command, might vary by OS/Docker version):
    ```bash
    docker volume inspect shared_logs_volume
    ```

5.  **Stop Services:**
    Press `Ctrl+C` in the terminal where `docker-compose up` is running. To remove the containers, networks, and volumes (optional, be careful with volumes if you want to persist data):
    ```bash
    docker-compose down -v
    ```

## Configuration Details

* **Kafka Broker**: `kafka:9092` (internal Docker network address)
* **Kafka Topic**: `log-topic` (auto-created if it doesn't exist)
* **Log File Path (inside containers)**: `/data/logs/app.log`
* **Shared Volume**: `shared_logs_volume` is used to share the log file between `file-logger` and `camel-quarkus-app`.

## Customization

* **Log Format/Frequency**: Modify `file-logger/logger.sh`.
* **Camel Route Logic**: Edit `camel-quarkus-app/src/main/java/org/acme/FileToKafkaRoute.java`. For example, you could add transformations (JSON, XML), filtering, etc.
* **Kafka Configuration**: Adjust settings in `docker-compose.yml` for the Kafka service or in `application.properties` for t