# Step 3: Using Spring Boot Test 

From this point, we can move our setup towards `@SpringBootTest`.
But why would we want to do this?
Using `@SpringBootTest` bring a couple quality-of-life improvements for us as developers, such as faster feedback cycles
(we don't have to rebuild the whole application and the image) or much easier debugging of the Java process.

## Step 3.a: Using the @SpringBootTest annotation
So let's first add the `@SpringBootTest` to our test class.
This will run our application before running the tests.
We can specify with the WebEnvironment configuration that the test should run on the default port 8080:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
```

To use this new port within our tests, we change the RestAssured test specification.
```java
protected RequestSpecification requestSpecification = new RequestSpecBuilder()
    .setPort(8080)
    .addHeader(
            HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_JSON_VALUE
    )
    .build();
```

## Step 3.b: Configuring the Spring Boot Test
Previously, we configured our application through environment variables.
Spring Boot used these variables to change its settings and connect to the right dependencies.
We now also have to provide the same configuration to our Spring Boot Test, which we can do through specifying a `DynamicPropertySource`:

```java
@DynamicPropertySource
public static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    registry.add("spring.kafka.bootstrap-servers", () -> "BROKER://localhost:" + kafka.getFirstMappedPort());
    registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + postgres.getFirstMappedPort() + "/workshop");
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "example");
}
```

This method then overwrites the configuration of Spring with the provided information.
Notice that now the URLs point to localhost, as we use the exposed ports on the host machine rather than the internal ports within the network.

## Step 3.c: Use pre-built images of Testcontainers
Currently we use `GenericContainer` to define all containers of our dependencies.
However, Testcontainers already provides multiple container definitions by itself, with a default configuration suited for common use cases.
As it so happens to be, for both Postgres and Kafka, Testcontainers already provides a definition through the `PostgreSQLContainer` and `ConfluentKafkaContainer` specifically.

### Using the PostgreSQLContainer 
The PostgreSQLContainer provides utility methods to get the URL to connect with, the username, and the password set.
Moreover, it provides a way to set the database name on the class.

To start off, replace the current `postgres` container with the following definition:
```java
@Container
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("workshop");
```

Now make sure that the Spring datasource configuration is set correctly, by supplying the right data through the `getJdbcUrl`, `getUsername`, and `getPassword` methods of the `postgres` container.

### Using the ConfluentKafkaContainer  
Similar to the PostgreSQLContainer, the ConfluentKafkaContainer provides us with nice utility methods to make configuring easier.
Moreover, the default configuration removes the explicit dependency on `zookeeper`, and therefore makes our setup overview less clustered.

To start off, replace both the `zookeeper` and `kafka` container definitions with the following definition:
```java
@Container
static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");
```

We also have to make sure that the bootstrap servers are known to Spring, for which we can use the `getBootstrapServers` method of the `kafka` container.

After this is done, we have now created a working setup.
Use `mvn integration-test` again to now see the Docker containers of the dependencies get started.
Then the Spring Boot Application is started, after which the tests are ran against this server.

## Step 3.d: Clean up
You might notice that the network is now only still used by the Redis container, which does not serve any purpose.
We can therefore remove both the network and the network-related builder method calls from the Redis container.