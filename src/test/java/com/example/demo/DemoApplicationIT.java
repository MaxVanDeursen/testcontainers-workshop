package com.example.demo;

import com.example.demo.model.Rating;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.io.File;

@Testcontainers
class DemoApplicationIT {

    @Container
    static ComposeContainer composeContainer = new ComposeContainer(new File("docker-compose.yml"))
            .withLocalCompose(true)
            .withExposedService("app-1", 8080, Wait.forHttp("/actuator/health"));

    protected RequestSpecification requestSpecification = new RequestSpecBuilder()
        .setBaseUri(String.format("http://%s:%d", composeContainer.getServiceHost("app-1", 8080), composeContainer.getServicePort("app-1", 8080)))
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