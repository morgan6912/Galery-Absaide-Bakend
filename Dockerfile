FROM amazoncorretto:17-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew buildFatJar --no-daemon --stacktrace

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p uploads
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]