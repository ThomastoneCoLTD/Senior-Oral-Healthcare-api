FROM amazoncorretto:17-alpine-jdk
LABEL maintainer="hana0142@thomastone.co.kr"

# 빌드 타임 ARG (기본값 dev)
ARG SPRING_PROFILES_ACTIVE=dev
# 런타임 ENV (docker run 시 -e 로 덮어쓰기 가능)
ENV SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE

# JAR 복사
RUN addgroup -S soh && adduser -S -G soh soh
WORKDIR /app
COPY --chown=soh:soh ./build/libs/*.jar application.jar
USER soh

# 실행
CMD ["java", "-jar", "./application.jar"]
