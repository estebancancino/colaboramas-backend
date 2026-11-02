
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# El jar queda de root y solo lectura; la app corre con UID/GID numérico sin privilegios
COPY --from=builder /app/target/api-service-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

# Rootfs en solo lectura: lo único escribible es /tmp (Tomcat y temporales de la JVM)
ENTRYPOINT ["java", "-Djava.io.tmpdir=/tmp", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
