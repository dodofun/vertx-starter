FROM dodofun/jdk11:latest

MAINTAINER  DODOFUN

# 项目代码
ADD deploy /opt/deploy

# 项目启动脚本
ADD dockerrun.sh /opt/run.sh

# 更新文件权限
RUN chmod -R 755 /opt/deploy
RUN chmod 755 /opt/run.sh

#设置系统编码
# RUN yum install kde-l10n-Chinese -y
# RUN yum install glibc-common -y
# RUN localedef -c -f UTF-8 -i zh_CN zh_CN.utf8
# ENV LC_ALL zh_CN.UTF-8
# ENV LANG zh_CN.UTF-8

# 环境变量
# ENV JAVA_HOME /opt/jdk
# ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
# ENV PATH $PATH:$JAVA_HOME/bin

# 端口号
EXPOSE 8080

#启动程序
CMD sh /opt/run.sh