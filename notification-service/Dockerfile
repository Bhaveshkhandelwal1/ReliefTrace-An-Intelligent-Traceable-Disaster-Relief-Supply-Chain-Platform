FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r drn && useradd -r -g drn -s /bin/false drn

COPY notification-service/target/notification-service-*.jar app.jar
RUN chown -R drn:drn /app

USER drn:drn

EXPOSE 8088

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
