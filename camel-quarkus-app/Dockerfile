# Stage 1: Build - Use the dedicated Quarkus Mandrel builder image for Java 21
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build

# Switch to the root user for installations
USER root

# Install necessary tools for downloading and extracting Maven
RUN microdnf update -y && microdnf install -y curl tar findutils gzip

# Define Maven version and download URL
ARG MAVEN_VERSION=3.9.6
ARG MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Download, extract, and configure Maven
RUN curl -fSL "${MAVEN_URL}" -o /tmp/apache-maven.tar.gz \
    && tar -xzf /tmp/apache-maven.tar.gz -C /opt/ \
    && rm /tmp/apache-maven.tar.gz \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven

# Set Maven environment variables
ENV M2_HOME=/opt/maven
ENV PATH="${M2_HOME}/bin:${PATH}"

# Set the working directory inside the container
WORKDIR /code

# Copy your project's source code into the /code directory
COPY . /code/

# Copy the custom settings.xml into Maven's conf directory
COPY settings.xml ${M2_HOME}/conf/settings.xml

# Clean the Maven local repository to prevent corrupted dependencies.
# This ensures a fresh download of all plugins and dependencies.
RUN rm -rf /root/.m2/repository || true && mkdir -p /root/.m2/repository

# Build the native executable
# -Pnative: Activates the native profile for native image compilation.
# -Dquarkus.package.type=native: Explicitly tells Quarkus to build a native executable.
# -DskipTests: Skips running tests during the build.
# -s ${M2_HOME}/conf/settings.xml: Explicitly tells Maven to use our custom settings.xml
RUN mvn package -Pnative -Dquarkus.package.type=native -DskipTests -s ${M2_HOME}/conf/settings.xml

# Stage 2: Runtime - Use a minimal Quarkus micro-image for the final application
FROM quay.io/quarkus/quarkus-micro-image:3.0

# Set the working directory inside the container to /work.
WORKDIR /work/

# Copy the native executable from the build stage to the runtime stage.
COPY --from=build /code/target/*-runner /work/application

# Make the application executable.
RUN chmod 775 /work/application

# Expose port 8080, which is the default HTTP port for Quarkus applications.
EXPOSE 8080

# Switch to a non-root user (user ID 1001) for security best practices.
USER 1001

# Define the command to run when the container starts.
ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]