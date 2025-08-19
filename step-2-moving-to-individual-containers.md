# Step 2: Moving to individual containers
Instead of defining the necessary services in the `docker-compose.yml` file, we will now declare them as Java objects.
Furthermore, we make use of the Docker networking feature, so that we can hardcode connection URLs and leverage the 
Docker DNS features.

## Step 2.a: Translate the containers the application depends on.
To do this, we translate each individual container within the docker-compose file into a `GenericContainer`. 
This class takes a image name as parameter.
To create a base for the Redis container, we can use the following code:
```java
static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
```

Use the following builder methods on the GenericContainer to further configure these containers:

| method | use case | docker-compose.yml equivalent |
| ------ | -------- | ----------------------------- |
| dependsOn | Specify the containers on which the defined container depends. | depends_on |
| waitingFor | Set the wait strategy. See the current `ComposeContainer` for an example of such wait strategy. | - |
| withEnv | Set an environment variable | environment |
| withExposedPorts | Expose the ports of the container to the containers within its network | expose |
| withNetworkAliases | Set the alias of the container to be used in Docker DNS. | container_name |

## Step 2.b: Specify the application as a GenericContainer
To make sure that the application itself is also run by Testcontainers as an individual container, we can create it with Testcontainers as well.
Rather than specifying the docker image name, instead we specify the Docker file on which the image should be based.
We can do that using the following code:
```java
@Container
static final GenericContainer<?> appContainer = new GenericContainer<>(
        new ImageFromDockerfile()
                .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))
                .withFileFromPath("target/demo-0.0.1-SNAPSHOT.jar", Paths.get("target/demo-0.0.1-SNAPSHOT.jar"))
)
```

After you have added this to your `DemoApplicationIT.java`, use the methods from Step 2.a to further configure the newly created GenericContainer.

## Step 2.c: Configuring the network
To ensure that the different containers within our definition can connect to eachother, we ensure they are in the same network.
We can specify a network with the following code:

```java
static Network network = Network.newNetwork();
```

Now use the `withNetwork` method on the `GenericContainer` to put each of our containers on the network. 

## Step 2.d: Changing RestAssured's request specification.

Now that we have moved away from our Docker compose file towards individual containers, we are able to get rid of the `ComposeContainer` from Step 1.
Before doing so, we have to make sure that we move the waiting strategy of our ComposeContainer to our `appContainer`.
We can do this with the `waitingFor` method, which takes a `WaitStrategy` that is currently defined in the `withExposedService`.
After this, we can remove our `composeContainer`. 
This also means that we should change our requestSpecification to use the information from our defined `appContainer`.:
```java
protected RequestSpecification requestSpecification = new RequestSpecBuilder()
    .setBaseUri(String.format("http://%s:%d", appContainer.getHost(), appContainer.getFirstMappedPort()))
    .addHeader(
            HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_JSON_VALUE
    )
    .build();
```

Run the test using `mvn integration-test`, it works again! 