# Fraud Detector

Fraud Detector is a Kotlin/Spring Boot service for real-time detection of potential fraud. It uses Kafka Streams for event processing, Elasticsearch for search and analytics, JPA/Hibernate with an H2 in-memory database for fast development, and Thymeleaf for a lightweight UI.

## Tech Stack
- Java 21, Kotlin 1.9.25, Spring Boot 3.5.3
- Spring Web, Spring Data JPA (H2 for development)
- Spring Kafka, Kafka Streams
- Elasticsearch 8 (Java client + REST client)
- Thymeleaf
- Smile ML (for analytics/ML experiments)
- Gradle Kotlin DSL

## Table of Contents
- [Quick Start](#quick-start)
- [Prerequisites](#prerequisites)
- [Run Locally](#run-locally)
- [Docker and Infrastructure](#docker-and-infrastructure)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Development](#development)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Quick Start

1) Start infrastructure (Kafka + Elasticsearch) via Docker Compose:
```bash
docker-compose up -d zookeeper broker elasticsearch
Copy
Insert

Run the app:
./gradlew bootRun
Copy
Insert

Optional: open H2 console (dev):
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:frauddb
User: sa
Password: (empty)
Verify Elasticsearch: http://localhost:9200
Configure Kafka clients/UI as needed.
Prerequisites
JDK 21
Docker + Docker Compose
Internet access (to fetch dependencies and container images)
Optional: cURL, Postman/Insomnia for API testing
Run Locally
With Gradle Wrapper
Start in dev mode (hot reload via Spring Devtools if enabled):
./gradlew bootRun
Copy
Insert

Build JAR and run:
./gradlew clean build
java -jar build/libs/fraud-detector-0.0.1-SNAPSHOT.jar
Copy
Insert

Build a Docker Image (Spring Boot Buildpacks)
Build an OCI image:
./gradlew bootBuildImage
Copy
Insert

Run the container (adjust env vars as needed):
docker run --rm -p 8080:8080 \
  -e SPRING_ELASTICSEARCH_URIS=http://host.docker.internal:9200 \
  -e APP_ELASTICSEARCH_ENABLED=true \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  fraud-detector:0.0.1-SNAPSHOT
Copy
Insert

Notes:

If you want the container to use the docker-compose network, attach it to the same network and reference services by DNS (e.g., broker:9092, http://elasticsearch:9200).
Docker and Infrastructure
A docker-compose.yaml is provided to spin up:

Zookeeper (Confluent 7.0.1)
Kafka broker (Confluent 7.0.1, ports 9092/9093)
Elasticsearch 8 (Bitnami, ports 9200/9300)
Start:

docker-compose up -d zookeeper broker elasticsearch
Copy
Insert

Stop:

docker-compose down
Copy
Insert

If you run the app outside Docker, use localhost ports. If you run it inside Docker, ensure it shares the network with the compose services and use service names (e.g., broker, elasticsearch).

Configuration
Default dev configuration (application.properties):

spring.datasource.url=jdbc:h2:mem:frauddb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true

spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

spring.elasticsearch.uris=http://localhost:9200
app.elasticsearch.enabled=true
app.elasticsearch.index-name=fraud-alerts

spring.kafka.bootstrap-servers=localhost:9092
Copy
Insert

Common environment overrides:

SPRING_ELASTICSEARCH_URIS
SPRING_ELASTICSEARCH_USERNAME, SPRING_ELASTICSEARCH_PASSWORD (if security enabled)
APP_ELASTICSEARCH_ENABLED
APP_ELASTICSEARCH_INDEX-NAME
SPRING_KAFKA_BOOTSTRAP_SERVERS
Example:

export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_ELASTICSEARCH_URIS=http://localhost:9200
export APP_ELASTICSEARCH_ENABLED=true
export APP_ELASTICSEARCH_INDEX-NAME=fraud-alerts
Copy
Insert

H2 console (dev):

URL: http://localhost:8080/h2-console
JDBC: jdbc:h2:mem:frauddb
User: sa, Password: (empty)
Architecture
High level:

Kafka: input streams of transaction events
Fraud Detector service: stream processing/analytics (Kafka Streams, optional ML), persistence, indexing
Elasticsearch: search and analytics over alerts/events
H2/JPA: fast development persistence (in-memory)
Thymeleaf: simple UI for inspection/experiments
Diagram:

graph TD
  A[Event Producers] -->|transactions| K[Kafka]
  K -->|stream processing| S[Fraud Detector (Spring Boot)]
  S -->|indexing| ES[(Elasticsearch)]
  S -->|persistence| DB[(H2/JPA)]
  S -->|UI| T[Thymeleaf]
Copy
Insert

Development
Language: Kotlin
Build: Gradle Kotlin DSL
Key dependencies:
spring-boot-starter-web, spring-boot-starter-data-jpa
spring-kafka, kafka-streams
elasticsearch-java, elasticsearch-rest-client
jackson-module-kotlin
thymeleaf
smile-core (2.6.0)
Devtools can be enabled for hot reload.
Run in dev:

./gradlew bootRun
Copy
Insert

Code style:

Kotlin conventions, ktlint, or detekt are recommended (not enforced by default).
Testing
Run tests:

./gradlew test
Copy
Insert

Test stack:

JUnit 5
spring-boot-starter-test
spring-kafka-test
Consider adding integration tests for Kafka and Elasticsearch if the service exposes endpoints/streams that need end-to-end verification.

Troubleshooting
Elasticsearch is not responding:
You can comment out the Elastic listener
Check container health: docker ps and health checks.
Configure Kafka clients/UI as needed.
