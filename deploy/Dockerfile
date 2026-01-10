# =========================
# Runtime Stage
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# CI 환경(./build/libs/)에서 빌드된 JAR를 복사
# build.gradle의 rootProject.name 또는 jar 이름에 따라 수정이 필요할 수 있으나, 일반적으로 *.jar로 매칭 가능
COPY build/libs/*.jar app.jar

# Spring 기본 포트
EXPOSE 8080

# Profile 설정 (기본값 local)
ARG PROFILE=local
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

# JVM 옵션 (필요하면 조정)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar /app/app.jar"]
