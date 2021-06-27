ARG version
FROM openjdk:11
MAINTAINER push-es-mappings

ENV HOST=localhost
ENV USER=elasticsearch
ENV PASSWORD="secret"
ENV CLUSTER="NOTGIVEN"

ADD target/push-es-mappings-${version}.jar /root/push-es-mappings.jar

RUN apt-get update \
  && chgrp -R 0 /root \
  && chmod -R g=u /root

WORKDIR /root

CMD java -jar /root/push-es-mappings.jar --host $HOST --cluster $CLUSTER --user $USER --password $PASSWORD
