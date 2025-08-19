# Step 4: Local Development environment with Testcontainers

We currently have the Testcontainers setup for our tests, but for local development we would still use the `docker-compose.yml` we started with.
This exercise will look at how we can use the Testcontainers configuration we use for our tests as well to run our application locally.

## Extracting environment to a Configuration

We'll use a [TestConfiguration](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/context/TestConfiguration.html) to encapsulate the environment creation.

Create a class `ContainerConfig` in the Test classpath that will implement the `TestConfiguration`:

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer redis() {
        return new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
    }
}
```

The example above contains a `@Bean` definition for a Redis container. 
Create similar `@Bean` definitions for `PostgresContainer` and `KafkaContainer`; you can copy the container configuration from the test classes.

Spring Boot 3.5 has integration with Testcontainers so containers exposed as `Bean` will be started by the framework automatically. 
The `ServiceConnection` helps Spring Boot to configure itself to use the services (similar to how the `@DynamicPropertySource` method before).

Now we'll use this configuration class to create the environment for our tests and running application locally.

## Running tests with the Context Initializer approach

Remove the container configration from the `DemoApplicationIT` class, and remove the `@DynamicPropertySource` method.

Instruct the test to use the configuration where containers are initialized as beans, by adding the `classes` property to the `SpringBootTest`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
  properties = {
  }, classes = {ContainerConfig.class})
```

Now you can check whether the tests still pass normally.

## Running your application with the Context Initializer approach

The `ContainerConfig` class is on the **test** classpath and unavailable to the application classpath.
This is by design because we don't want to make Testcontainers and other testing libraries available in the application classpath.

So to use the `ContainerConfig`, we need to create a separate entry point for running the application.
Create a `TestDemoApplication` class, that uses the actual `DemoApplication` and also uses the `ContainerConfig` we implemented above: 

```java
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication
                .from(DemoApplication::main)
                .with(ContainerConfig.class)
                .run(args);
    }
}
```

Running this class will run the `DemoApplication`, and you should see from the logs that the application is started successfully.
Stopping the application will remove the containers just like Testcontainers cleans up the environment after running the tests.

## Hint 1:

You can detach the lifecycle of the containers from the lifecycle of the Spring context by using Dev tools.
Add the `spring-boot-devtools` dependency.
Annotate beans and bean methods with `@RestartScope`.

When reloading the project changes with devtools you can see that the containers are not restarted.

## Connecting to the Database
Once the application is running, you might want to connect to the database to inspect the data in the database.
By default, Testcontainers starts the containers and map it to a random available port on the host.
So, you need to find out the mapped port to connect to the database.

Instead, we can use Testcontainers Desktop fixed ports support to connect to the database.

Open Testcontainers Desktop, and select the `Services` -> `Open config location`.
It will open a directory with the example configuration files for commonly used services.

Copy the `postgres.toml.example` to `postgres.toml`, and update it's content to the following:

```toml
ports = [
  {local-port = 5432, container-port = 5432},
]

selector.image-names = ["postgres"]
```

This configuration will map Postgres container port 5432 to the host port 5432.
Now, when you run the application, you can connect to the database using the following connection details:

```
host: localhost
port: 5432
username: test
password: test
database: test
```

## Reusable Containers
During the development of the application, you might want to stop and start the application multiple times.
Instead of creating the containers from scratch every time, you can use the `reuse` feature of Testcontainers.

* Enable the `reuse` feature in Testcontainers Desktop by enabling **Preferences** -> **Enable reusable containers**.
* Update the Containers configuration to use the `reuse` feature with `.withReuse(true)`:

```java
@Bean
@ServiceConnection
public PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>("postgres:16-alpine").withReuse(true);
}
```

Now, when you restart the application, you can see that the container is reused from the previous run.
BY enabling the `reuse` feature, Testcontainers won't remove those reusable containers automatically 
when the application is stopped or test execution is done.

If you no longer need the container, you can remove it from the Testcontainers Desktop -> **Terminate Containers**.

## Freezing containers to prevent their shutdown to debug
When you run Testcontainers tests, the containers are automatically stopped and removed after the test execution is done.
This is a great feature to keep the environment clean and prevent resource leaks.
But, sometimes you might want to debug the test and inspect the data in the database or the messages in the Kafka topic.

You can use the Testcontainers Desktop **Freeze containers shutdown** feature 
that will prevent the container shutdown allowing you to debug the issue.
