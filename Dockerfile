FROM dodofun/jdk11:latest

MAINTAINER  DODOFUN

# 项目代码
ADD deploy /opt/deploy

# 项目启动脚本
ADD dockerrun.sh /opt/run.sh

# 更新文件权限
RUN chmod -R 755 /opt/deploy
RUN chmod 755 /opt/run.sh

# 环境变量
ENV JAVA_HOME /opt/jdk
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV PATH $PATH:$JAVA_HOME/bin
