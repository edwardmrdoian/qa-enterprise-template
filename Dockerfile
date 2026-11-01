# Run the whole suite in a container with browsers preinstalled.
#   docker build -t qa-tests .
#   docker run --rm -v "$PWD/target:/tests/target" qa-tests
# Keep the image tag in sync with <playwright.version> in pom.xml.
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /tests
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
CMD ["mvn", "-B", "test", "-Dheadless=true", "-Dbrowser=chromium"]
