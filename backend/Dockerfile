# Build context: ./backend
FROM gradle:8.9-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home payfriend
COPY --from=build /home/gradle/src/build/libs/payfriend-backend-all.jar /app/server.jar
USER payfriend
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
