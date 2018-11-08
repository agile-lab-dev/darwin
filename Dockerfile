FROM mariadb:10.3

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

ENV SBT_VERSION 1.2.6

RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean

RUN curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb && \
    apt-get update && \
    apt-get install sbt && \
    sbt sbtVersion

# Install Git
RUN apt-get update && \
    apt-get install -y git && \
    apt-get clean;

RUN git clone -b mysqlconnector https://github.com/pier485/darwin.git

WORKDIR /darwin

RUN chmod 700 /darwin/init-db-and-launch-tests.sh

#CMD sbt darwin-mysql-connector/test