## Stage 1: Build the native executable
# Use a Maven image that includes GraalVM for native compilation
# Ensure the GraalVM version is compatible with your Quarkus version.
# For Quarkus 3.x, Java 17+ is typical.
# Check https://quarkus.io/guides/building-native-image#configuring-graalvm for recommended images.
# Example: quay.io/quarkus/ubi-quarkus-native-image:23.0-java17 for Quarkus 3.2+
# Or use a generic Maven image with a JDK and let Quarkus download GraalVM.
ARG QUARKUS_NATIVE_BUILDER_IMAGE=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-17
FROM ${QUARKUS_NATIVE_BUILDER_IMAGE} AS build

# Set the working directory
WORKDIR /usr/src/app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the native executable
# Ensure MAVEN_OPTS is set if your build needs more memory
# RUN mvn -B package -Dnative -Dquarkus.native.container-build=true -DskipTests
# Adding -Dquarkus.native.builder-image to ensure it uses the one specified or a compatible one
# The -B flag is for batch mode (non-interactive)
RUN mvn -B package -Pnative -Dquarkus.native.container-build=true -DskipTests

## Stage 2: Create the lightweight runtime image
# Use a minimal base image. Quarkus provides ubi-quarkus-micro-image or similar.
# For Quarkus 3.x, quarkus-micro-image is based on ubi9-micro.
FROM quay.io/quarkus/quarkus-micro-image:1.0
# FROM registry.access.redhat.com/ubi8/ubi-minimal AS runtime
# FROM alpine:latest AS runtime # If you prefer alpine, ensure all native libs are there

WORKDIR /work/

# Copy the native executable from the build stage
# The executable is typically in target/*-runner
COPY --from=build /usr/src/app/target/*-runner /work/application

# Set permissions for the executable and expose port
RUN chmod 775 /work/application && chown 1001 /work/application
EXPOSE 8080
USER 1001

# Set the entrypoint to run the native executable
# The JAVA_OPTS_APPEND can be used to pass additional Java options if needed
# For native, these are usually baked in or passed differently.
# For this app, environment variables are used for configuration.
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
