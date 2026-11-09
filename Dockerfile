# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐싱: 빌드 설정만 먼저 복사해 의존성을 받아둔다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

# 소스 복사 후 실행 가능한 jar 생성 (테스트는 CI에서 수행)
COPY src src
RUN ./gradlew bootJar --no-daemon \
    && cp "$(ls build/libs/*.jar | grep -v plain)" app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root가 아닌 전용 사용자로 실행
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
