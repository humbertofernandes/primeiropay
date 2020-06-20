FROM openjdk:13-ea-27-jdk-alpine3.10
LABEL maintainer="humbertotpgfernandes@gmail.com"

ENV LANG C.UTF-8

RUN apk add --update bash

ADD target/primeiropay-1.0.0-SNAPSHOT-fat.jar /app/app.jar

CMD java -jar /app/app.jar
