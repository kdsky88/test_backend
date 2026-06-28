# Render(및 임의 Docker 호스트)용 백엔드 이미지
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render는 PORT 환경변수를 주입 → application.yml의 server.port=${PORT:8080}가 사용
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
