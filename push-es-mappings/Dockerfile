# Sadly, this file is not really used curently. See https://git.vpro.nl/npo/docker/es-builder/-/blob/master/Dockerfile instead
ARG POM_VERSION
FROM openjdk:11
MAINTAINER push-es-mappings

ENV HOST=localhost
ENV USER=elasticsearch
ENV PASSWORD="secret"
ENV CLUSTER="NOTGIVEN"
ENV EXPERIMENTAL="false"

ADD target/push-es-mappings-$POM_VERSION.jar /root/push-es-mappings.jar

RUN apt-get update \
  && chgrp -R 0 /root \
  && chmod -R g=u /root

WORKDIR /root

CMD java -jar /root/push-es-mappings.jar --host $HOST --cluster $CLUSTER --user $USER --password $PASSWORD $(if [ "$EXPERIMENTAL" = "true" ]; then echo "--experimental";  fi)
