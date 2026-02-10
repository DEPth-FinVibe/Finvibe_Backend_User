# =========================
# Builder Stage
# =========================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew clean bootJar -x test

# =========================
# Runtime Stage
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ARG PROFILE=local
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar /app/app.jar"]
