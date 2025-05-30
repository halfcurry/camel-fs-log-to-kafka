package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.LoggingLevel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;

@ApplicationScoped
public class FileToKafkaRoute extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(FileToKafkaRoute.class);

    @ConfigProperty(name = "log.file.path", defaultValue = "/data/logs/app.log")
    String logFilePath; // Injected from application.properties or env

    @ConfigProperty(name = "checkpoint.dir.path", defaultValue = "/data/checkpoint")
    String checkpointDirPath; // Injected, for idempotent repository

    @ConfigProperty(name = "camel.quarkus.kafka.brokers", defaultValue = "localhost:9092")
    String kafkaBrokers;

    // Helper method to calculate SHA-256 hash for a string
    private String calculateSHA256(String text) {
        if (text == null) {
            return ""; // Or throw an IllegalArgumentException
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("SHA-256 algorithm not found", e);
            // Fallback or rethrow, depending on desired error handling
            // For simplicity, returning a basic hash or a constant error string
            // In a real app, this should be handled more robustly.
            return "error_hash_" + text.hashCode();
        }
    }

    @Produces
    @Named("lineIdempotentRepository")
    public FileIdempotentRepository createFileIdempotentRepository() {
        FileIdempotentRepository repository = new FileIdempotentRepository();
        File storeFile = new File(checkpointDirPath, "processedLineHashes.dat");
        LOG.info("Initializing FileIdempotentRepository at: {}", storeFile.getAbsolutePath());
        repository.setFileStore(storeFile);
        repository.setCacheSize(5000); // Number of unique line hashes to keep in memory
        // repository.setFlushOnUpdate(true); // Ensure every update is written to disk immediately (slower but safer)
        // Ensure the directory exists
        if (!storeFile.getParentFile().exists()) {
            boolean created = storeFile.getParentFile().mkdirs();
            if (created) {
                LOG.info("Created checkpoint directory: {}", storeFile.getParentFile().getAbsolutePath());
            } else {
                LOG.warn("Failed to create checkpoint directory or it already exists: {}", storeFile.getParentFile().getAbsolutePath());
            }
        }
        return repository;
    }

    @Override
    public void configure() throws Exception {
        // File component URI
        // noop=true: leave the file as is after processing.
        // idempotent=false: handled by idempotentConsumer EIP later with our custom repository.
        // We set idempotent on the consumer EIP, not directly on the file component here.
        // The file component will re-read the entire file on each poll if it changed.
        // The idempotent consumer will then filter out already processed lines.
        // initialDelay=1000, delay=2000: poll every 2 seconds after an initial 1-second delay.
        String fileEndpointUri = String.format(
            "file:%s?noop=true&initialDelay=1000&delay=2000&fileName=%s&charset=UTF-8",
            new File(logFilePath).getParent(), // Directory
            new File(logFilePath).getName()     // File name
        );

        String kafkaEndpointUri = String.format(
            "kafka:log-topic?brokers=%s",
            kafkaBrokers
        );

        LOG.info("Configuring Camel route: File [{}] -> Kafka [{}] on topic [log-topic]", logFilePath, kafkaBrokers);
        LOG.info("Checkpoint data will be stored in directory: [{}]", checkpointDirPath);

        from(fileEndpointUri)
            .routeId("file-to-kafka-route")
            .split(body().tokenize("\n")).streaming() // Process lines one by one, streaming helps with large files
                .transform(body().regexReplaceAll("[\r\n]+", "")) // Clean up any trailing newlines/CR
                .filter(body().isNotEqualTo("")) // Skip empty lines that might result from splitting
                .process(exchange -> {
                    String line = exchange.getIn().getBody(String.class);
                    String lineHash = calculateSHA256(line);
                    exchange.getIn().setHeader("lineHash", lineHash);
                    // LOG.trace("Hashed line for idempotency: {} -> {}", line, lineHash); // Very verbose
                })
                // Use the lineHash header for the idempotent check with the configured repository
                .idempotentConsumer(header("lineHash")).idempotentRepository("lineIdempotentRepository")
                    // If the line (based on its hash) is new, it passes through
                    .log(LoggingLevel.DEBUG, "Processing new line (hash: ${header.lineHash}): ${body}")
                    .to(kafkaEndpointUri)
                    .log(LoggingLevel.INFO, "Sent to Kafka (hash: ${header.lineHash}): ${body}")
            .end(); // End of split

        LOG.info("Camel route configured successfully.");
    }
}