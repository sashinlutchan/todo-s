FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY mvnw pom.xml mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
