# 1단계: 빌드 (gradle로 jar 생성) 
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# gradle wrapper와 설정 먼저 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# 소스 복사 후 빌드 (테스트 제외하고 jar만)
COPY src src
RUN ./gradlew clean bootJar -x test

# 2단계: 실행 (가벼운 JRE 이미지에 jar만 복사)
FROM --platform=linux/amd64 eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]