package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FileToKafkaRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(FileToKafkaRoute.class);

    // Inject the log file path from application.properties or environment variables
    // The environment variable LOG_FILE_PATH from docker-compose.yml will override application.properties
    @ConfigProperty(name = "log.file.path", defaultValue = "/data/logs/app.log")
    String logFilePath;

    // Inject Kafka brokers from application.properties or environment variables
    // The environment variable CAMEL_QUARKUS_KAFKA_BROKERS from docker-compose.yml will override application.properties
    @ConfigProperty(name = "camel.quarkus.kafka.brokers", defaultValue = "localhost:9092")
    String kafkaBrokers;

    @Override
    public void configure() throws Exception {
        // Construct the file endpoint URI dynamically
        // stream:file is suitable for tailing files.
        // fileWatcher=true uses JDK's WatchService for efficient file change detection.
        // scanStream=true re-scans the file if it's rolled over or recreated.
        // scanStreamDelay is the polling delay if fileWatcher is not effective or for initial scan.
        String fileEndpointUri = String.format(
            "stream:file?fileName=%s&fileWatcher=true&scanStream=true&scanStreamDelay=1000",
            logFilePath
        );

        // Construct the Kafka endpoint URI dynamically
        String kafkaEndpointUri = String.format(
            "kafka:log-topic?brokers=%s",
            kafkaBrokers
        );

        LOG.info("Starting Camel route: File [{}] -> Kafka [{}] on topic [log-topic]", logFilePath, kafkaBrokers);

        from(fileEndpointUri)
            .routeId("file-to-kafka-route")
            .convertBodyTo(String.class) // Ensure the body is a String
            .filter(body().isNotNull())   // Process only non-null lines
            .filter(body().isNotEqualTo("")) // Process only non-empty lines
            .process(exchange -> {
                String line = exchange.getIn().getBody(String.class);
                // You can add more sophisticated processing here if needed
                // For example, parsing structured logs (JSON, etc.)
                LOG.debug("Read from file: {}", line);
            })
            .log("Processing line: ${body}") // Log the line being processed by Camel
            .to(kafkaEndpointUri)
            .log("Sent to Kafka: ${body}"); // Log after sending to Kafka

        LOG.info("Camel route configured: {} -> {}", fileEndpointUri, kafkaEndpointUri);
    }
}
