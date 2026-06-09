FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY settings.xml ./settings.xml
COPY pom.xml ./pom.xml
RUN mvn -s settings.xml dependency:go-offline -B
COPY src ./src
RUN mvn -s settings.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
