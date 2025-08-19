# Testcontainers workshop
This workshop is heavily inspired by the [workshop from Testcontainers themselves](https://github.com/testcontainers/workshop).
In particular, the application code and dependency structure from this repository is used. 
The exercises that are included in this workshop are also present in the original workshop, albeit formatted differently.

# Prerequisites
- Java 17 or newer
  You can check the Java version by running:
  ```text
  $ java -version

  openjdk version "21.0.6" 2025-01-21 LTS
  OpenJDK Runtime Environment Zulu21.40+17-CA (build 21.0.6+7-LTS)
  OpenJDK 64-Bit Server VM Zulu21.40+17-CA (build 21.0.6+7-LTS, mixed mode, sharing)
  ```
- Supported Docker environment, like **Docker Desktop**, **OrbStack**, **Rancher Desktop**, etc.
  You can check the Docker availability by running: 
  ```text
  $ docker version

  Client:
  Cloud integration: v1.0.22
  Version:           20.10.11
  API version:       1.41
  Go version:        go1.16.10
  Git commit:        dea9396
  Built:             Thu Nov 18 00:42:51 2021
  OS/Arch:           windows/amd64
  Context:           default
  Server: Docker Engine - Community
  Engine:
    Version:          20.10.11
    API version:      1.41 (minimum version 1.12)
    Go version:       go1.16.9
    Git commit:       847da18
    Built:            Thu Nov 18 00:35:39 2021
    OS/Arch:          linux/amd64
    Experimental:     false
    ...
  ```

## Download the project

Clone the following project from GitHub to your computer:  
[https://github.com/maxvandeursen/testcontainers-workshop](https://github.com/maxvandeursen/testcontainers-workshop)

## Build the project to download the dependencies

With Maven:
```text
./mvnw package
```

## \(optionally\) Pull the required images before doing the workshop

This might be helpful if the internet connection at the workshop venue is somewhat slow.

```text
docker pull postgres:16-alpine
docker pull redis:7-alpine
docker pull openjdk:21
docker pull confluentinc/cp-kafka:7.5.0
```

# Application Introduction

The app is a simple microservice based on Spring Boot for rating conference talks. It provides an API to track the ratings of the talks in real time.

## Storage

### SQL database with the talks

When a rating is submitted, we must verify that the talk for the given ID is present in our database.

Our database of choice is PostgreSQL, accessed with Spring JDBC.

Check `com.example.demo.repository.TalksRepository`.

### Redis

We store the ratings in Redis database with Spring Data Redis.

Check `com.example.demo.repository.RatingsRepository`.

### Kafka

We use ES/CQRS to materialize the events into the state. Kafka acts as a broker and we use Spring Kafka.

Check `com.example.demo.streams.RatingsListener`.

## API

The API is a Spring Web REST controller \(`com.example.demo.api.RatingsController`\) and exposes two endpoints:

* `POST /ratings { "talkId": ?, "value": 1-5 }` to add a rating for a talk
* `GET /ratings?talkId=?` to get the histogram of ratings of the given talk

# Local development environment

The local development environment is created using Docker Compose.

1. Build the application with `mvn package`.
2. Use `docker-compose up -d`. The application is now reachable at `localhost:8080`.

# Running the tests

The `DemoApplicationIT.java` contains three tests which currently depend on the Docker Compose running.
First run `docker-compose up -d` and then run `mvn integration-test` to test the application. 

### The first exercise
[Go to the first exercise](step-1-lift-and-shift.md)