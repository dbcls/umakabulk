FROM java:8

ENV SCALA_VERSION 2.11.7
ENV SBT_VERSION 0.13.11
ENV JAVA_OPTS="-Xms2G -Xmx8G -Xss2M -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"

RUN \
  curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

RUN mkdir /sbMeta
ADD . /sbMeta
WORKDIR /sbMeta

RUN sbt compile
