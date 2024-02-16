FROM amazoncorretto:21-alpine

MAINTAINER push-es-mappings

ARG NAME=push-es-mappings

ARG PROJECT_VERSION

ENV HOST=localhost
ENV USER=elasticsearch
ENV PASSWORD="secret"
ENV CLUSTER="NOTGIVEN"
ENV EXPERIMENTAL="false"

RUN apk upgrade \
  && chgrp -R 0 /root \
  && chmod -R g=u /root


ADD target/push-es-mappings-${PROJECT_VERSION}.jar /root/push-es-mappings-${PROJECT_VERSION}.jar
RUN ln -s  /root/push-es-mappings-${PROJECT_VERSION}.jar /root/push-es-mappings.jar

WORKDIR /root

CMD java -jar /root/push-es-mappings.jar --host "$HOST" --cluster "$CLUSTER" --user "$USER" --password "$PASSWORD" $(if [ "$EXPERIMENTAL" = "true" ]; then echo "--experimental";  fi) ; echo "sleeping for inspection" ; sleep $(expr 3600 \* 24 \* 2)
