FROM maven:3.8.4-openjdk-17-slim AS build
LABEL maintainer="tinyX-devs"
WORKDIR /app

COPY pom.xml .
COPY common-tools/pom.xml ./common-tools/
COPY srvc-search/pom.xml ./srvc-search/
COPY repo-post/pom.xml ./repo-post/
COPY repo-social/pom.xml ./repo-social/
COPY srvc-user/pom.xml ./srvc-user/
COPY srvc-user-timeline/pom.xml ./srvc-user-timeline/
COPY srvc-home-timeline/pom.xml ./srvc-home-timeline/

RUN mvn install -pl common-tools && mvn dependency:go-offline -pl repo-post -am

COPY common-tools ./common-tools
COPY srvc-search ./srvc-search
COPY srvc-user ./srvc-user
COPY repo-post ./repo-post
COPY repo-social ./repo-social
COPY srvc-user-timeline ./srvc-user-timeline
COPY srvc-home-timeline ./srvc-home-timeline

RUN mvn clean package -pl repo-post -am -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/repo-post/target/quarkus-app/quarkus-run.jar ./app.jar
COPY --from=build /app/repo-post/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/repo-post/target/quarkus-app/app/ ./app/
COPY --from=build /app/repo-post/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
ENTRYPOINT ["java", "-Dquarkus.profile=prod", "-jar", "app.jar"]
