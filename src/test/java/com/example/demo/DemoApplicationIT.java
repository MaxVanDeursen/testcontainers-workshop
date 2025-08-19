package com.example.demo;

import com.example.demo.model.Rating;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;

@Testcontainers
class DemoApplicationIT {
    static Network network = Network.newNetwork();

    @Container
    static final GenericContainer<?> zookeeper = new GenericContainer<>("confluentinc/cp-zookeeper:7.5.0")
            .withExposedPorts(2181)
            .withNetwork(network)
            .withNetworkAliases("zookeeper")
            .withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
            .withEnv("ZOOKEEPER_TICK_TIME", "2000");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withNetwork(network)
            .withNetworkAliases("redis");

    @Container
    static final GenericContainer<?> kafka = new GenericContainer<>("confluentinc/cp-kafka:7.5.0")
            .withExposedPorts(9093)
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9093")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
            .dependsOn(zookeeper);

    @Container
    static final GenericContainer<?> postgres = new GenericContainer<>("postgres:16-alpine")
            .withExposedPorts(5432)
            .withNetwork(network)
            .withNetworkAliases("db")
            .withEnv("POSTGRES_PASSWORD", "example")
            .withEnv("POSTGRES_DB", "workshop");

    @Container
    static final GenericContainer<?> appContainer = new GenericContainer<>(
            new ImageFromDockerfile()
                    .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))
                    .withFileFromPath("target/demo-0.0.1-SNAPSHOT.jar", Paths.get("target/demo-0.0.1-SNAPSHOT.jar"))
    )
        .withExposedPorts(8080)
        .withEnv("SPRING_DATA_REDIS_HOST", "redis")
        .withEnv("SPRING_DATA_REDIS_PORT", "6379")
        .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", "BROKER://kafka:9093")
        .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/workshop")
        .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
        .withEnv("SPRING_DATASOURCE_PASSWORD", "example")
        .withNetwork(network)
        .waitingFor(Wait.forHttp("/actuator/health"))
        .dependsOn(redis, kafka, postgres);

    protected RequestSpecification requestSpecification = new RequestSpecBuilder()
        .setBaseUri(String.format("http://%s:%d", appContainer.getHost(), appContainer.getFirstMappedPort()))
        .addHeader(
                HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE
        )
        .build();

    @Test
    void testSingleRating() {
        String talkId = "single-rating";

        given(requestSpecification)
                .body(new Rating(talkId, 5))
                .when()
                .post("/ratings")
                .then()
                .statusCode(202);

        await().untilAsserted(() -> {
            given(requestSpecification)
                    .queryParam("talkId", talkId)
                    .when()
                    .get("/ratings")
                    .then()
                    .body("5", is(1));
        });

    }

    @Test
    void testRatingsWithMultipleTalks() {
        String talkId = "multiple-ratings";

        for (int i = 1; i <= 5; i++) {
            given(requestSpecification)
                    .body(new Rating(talkId, i))
                    .when()
                    .post("/ratings");
        }

        await().untilAsserted(() -> {
            given(requestSpecification)
                    .queryParam("talkId", talkId)
                    .when()
                    .get("/ratings")
                    .then()
                    .body("1", is(1))
                    .body("2", is(1))
                    .body("3", is(1))
                    .body("4", is(1))
                    .body("5", is(1));
        });
    }

    @Test
    void testUnknownTalk() {
        String talkId = "nonexistent-talk";

        given(requestSpecification)
                .body(new Rating(talkId, 5))
                .when()
                .post("/ratings")
                .then()
                .statusCode(404);
    }
}