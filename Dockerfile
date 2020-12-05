FROM maven:3.6.3-openjdk-16-slim as build

ENV LOC .
ENV SRC s3_example.json

LABEL maintainer="bblommers"
COPY src /tmp/partiql/src
COPY pom.xml /tmp/partiql
COPY ${LOC}/${SRC} /tmp/partiqldata/${SRC}
RUN mvn -f /tmp/partiql/pom.xml clean compile assembly:single
RUN pwd
RUN ls -la /tmp/partiqldata


FROM gcr.io/distroless/java
COPY --from=build /tmp/partiql/target/partiql.s3.client-0.0.1-SNAPSHOT-jar-with-dependencies.jar /tmp/partiql/client.jar
COPY --from=build /tmp/partiqldata/${SRC} /tmp/partiqldata/${SRC}
ENTRYPOINT ["java","-jar","/tmp/partiql/client.jar"]
