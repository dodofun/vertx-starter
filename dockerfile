FROM dodofun/jdk11:lastest

MAINTAINER  DODOFUN

ADD deploy /opt/deploy
ADD run.sh /opt/

RUN chmod -R 755 /opt/deploy
RUN chmod 755 /opt/run.sh

ENV JAVA_HOME /opt/jdk
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV PATH $PATH:$JAVA_HOME/bin
