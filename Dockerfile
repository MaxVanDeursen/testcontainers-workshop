FROM openjdk:21-bookworm
RUN addgroup --system spring \
    && adduser --system --group --home /home/spring --disabled-password --shell /bin/bash spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]