FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S nexpay && adduser -S nexpay -G nexpay
USER nexpay
COPY target/*.jar app.jar
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "app.jar"]
