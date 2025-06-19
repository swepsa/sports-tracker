# 🏟️ Sports Tracker

A Spring Boot microservice for tracking live sports events.  
It periodically fetches score updates from an external API and publishes them to a Kafka topic.

---

## 🚀 Features

- ✅ Java 21 with virtual threads
- ✅ Spring Boot 3
- ✅ Kafka integration (Testcontainers for integration tests)
- ✅ Mocked external HTTP API using MockWebServer
- ✅ REST endpoint for event status updates
- ✅ Integration and unit tests with JUnit 5 and Awaitility

---

## 🛠️ Tech Stack

| Layer            | Technology                                     |
|------------------|------------------------------------------------|
| Language         | Java 21                                        |
| Build Tool       | Gradle                                         |
| Framework        | Spring Boot 3                                  |
| Messaging        | Apache Kafka                                   |
| Testing          | JUnit 5, Testcontainers, MockWebServer         |
| API Docs         | OpenAPI 3 (Swagger)                            |

---

## 📦 Requirements

- Java 21
- Gradle 8.x
- Docker (for Kafka Testcontainers)
- (Optional) IntelliJ IDEA or similar IDE

---

## 📥 Installation

Clone the repository:

```bash
git clone https://github.com/swepsa/sports-tracker.git
cd sports-tracker
```

---

## ⚙️ Running the Application

```bash
./gradlew bootRun
```

By default, it runs on:

- `http://localhost:8080`
- External API mock endpoint: `http://localhost:8081/api/events/{eventId}/score`

---

## 🧪 Running Tests

```bash
./gradlew test
```

This will:
- Start a temporary Kafka broker using Testcontainers
- Start a mock HTTP server
- Run both unit and integration tests

---

## 🧵 Virtual Threads (Java 21)

The event scheduler uses **virtual threads** via Java 21 to run concurrent polling tasks efficiently without consuming platform threads.

---

## 📡 API Endpoints

| Method | Endpoint           | Description                    |
|--------|--------------------|--------------------------------|
| POST   | `/events/status`   | Update event "live"/"not live" |

Swagger UI is available at:  
`http://localhost:8080/swagger-ui.html`

---

## 🔄 Kafka Topic

| Topic Name          | Description                   |
|---------------------|-------------------------------|
| `live-sports-events` | Publishes live event updates  |

---

## 📂 Configuration

### `application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    template:
      default-topic: live-sports-events

external:
  api:
    url: http://localhost:8081/api/events/{eventId}/score
```

---

## 🧪 Testcontainers Setup

Kafka is started automatically during integration tests:

```java
static {
    kafkaContainer.start();
}
```

Kafka bootstrap servers are injected via:

```java
TestPropertyValues.of("spring.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers())
```

---

## 🧾 License

[MIT License](LICENSE)

---

## 👨‍💻 Author

Developed by [Yuri Ulyanov](https://github.com/swepsa)