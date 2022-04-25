FROM openjdk:11

#RUN mkdir /opt
RUN mkdir /opt/kv

RUN set -eux; \
    apt-get install bash; 

COPY . /opt/kv
WORKDIR /opt/kv

ARG SCRIPT=""
ARG PARAMETRS=""

#RUN /bin/bash -c $SCRIPT $PARAMETRS

#RUN bash $SCRIPT $PARAMETRS
CMD /bin/sh $SCRIPT $PARAMETRS
