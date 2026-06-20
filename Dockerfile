# ---------- build stage ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache dependencies first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Build the application
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/zoio-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9999

# Container-aware heap sizing: use a share of the cgroup memory limit set in
# docker-compose. Virtual threads are enabled via application.yaml.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
