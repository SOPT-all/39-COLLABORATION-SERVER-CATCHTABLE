# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 캐싱: 빌드 설정만 먼저 복사해 의존성을 받아둔다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew downloadDependencies --no-daemon

# 소스 복사 후 실행 가능한 jar 생성 (테스트는 CI에서 수행)
COPY src src
RUN ./gradlew bootJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root가 아닌 전용 사용자로 실행
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /workspace/build/libs/app.jar app.jar
USER app

# 힙 상한 고정: 1GB EC2 기준, 사양이 다르면 실행 시 -e JAVA_OPTS로 덮어쓴다
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-Xmx512m"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
