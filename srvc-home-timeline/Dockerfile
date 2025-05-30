FROM maven:3.8.4-openjdk-17-slim AS build
LABEL maintainer="tinyX-devs"
WORKDIR /app

COPY pom.xml .
COPY common-tools/pom.xml ./common-tools/
COPY srvc-user/pom.xml ./srvc-user/
COPY srvc-search/pom.xml ./srvc-search/
COPY repo-post/pom.xml ./repo-post/
COPY repo-social/pom.xml ./repo-social/
COPY srvc-home-timeline/pom.xml ./srvc-home-timeline/
COPY srvc-user-timeline/pom.xml ./srvc-user-timeline/

RUN mvn install -pl common-tools && mvn dependency:go-offline -pl srvc-home-timeline -am

COPY common-tools ./common-tools
COPY srvc-home-timeline ./srvc-home-timeline
COPY srvc-user ./srvc-user
COPY srvc-search ./srvc-search
COPY repo-post ./repo-post
COPY repo-social ./repo-social

RUN mvn clean package -pl srvc-home-timeline -am -DskipTests

FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/srvc-home-timeline/target/quarkus-app/quarkus-run.jar ./app.jar
COPY --from=build /app/srvc-home-timeline/target/quarkus-app/lib/ ./lib/
COPY --from=build /app/srvc-home-timeline/target/quarkus-app/app/ ./app/
COPY --from=build /app/srvc-home-timeline/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]