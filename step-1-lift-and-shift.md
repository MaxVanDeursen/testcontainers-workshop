# Step 1: Getting Started
In order to tightly integrate the lifecycle of our test environment with the lifecycle of our tests,
we can already integrate Testcontainers and still make use of our existing `docker-compose.yml`:

```java
@Container
static DockerComposeContainer composeContainer = new DockerComposeContainer(new File("docker-compose.yml"))
        .withLocalCompose(true)
        .withExposedService("app-1", 8080)
        .waitingFor("app-1", Wait.forHttp("/actuator/health"));
```

You also need to add the `@Testcontainers` annotation to the test class, if you want the [Testcontainers-JUnit-Jupiter extension](https://www.testcontainers.org/test_framework_integration/junit_5/)
to manage the container lifecycle (similar to how we did in step 8).

Finally, make sure to configure RestAssured to access the dynamic port exposed by Testcontainers:

```java
requestSpecification = new RequestSpecBuilder()
    .setBaseUri(String.format("http://%s:%d", composeContainer.getServiceHost("app-1", 8080), composeContainer.getServicePort("app-1", 8080)))
    .addHeader(
            HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_JSON_VALUE
    )
    .build();
```

Run the test from the IDE, it works! 

Note how you don't need to run `docker compose up` before the test, or manually clean up the environment after.