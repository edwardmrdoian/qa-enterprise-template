# Run the whole suite in a container with browsers preinstalled.
#   docker build -t qa-tests .
#   docker run --rm -v "$PWD/target:/tests/target" qa-tests
# Keep the image tag in sync with <playwright.version> in pom.xml.
FROM eclipse-temurin:25-jdk-noble AS jdk
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /tests
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src ./src
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENTRYPOINT ["./mvnw", "-B"]
CMD ["clean", "verify", "-Dheadless=true", "-Dbrowser=chromium"]
