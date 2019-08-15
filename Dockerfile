FROM dodofun/jdk:latest

MAINTAINER  DODOFUN

# 项目代码
ADD lib /opt/deploy/lib
ADD etc /opt/deploy/etc
ADD bin /opt/deploy/bin
ADD log /opt/deploy/log

# 项目启动脚本
ADD dockerrun.sh /opt/run.sh

# 更新文件权限
RUN chmod -R 755 /opt/deploy
RUN chmod 755 /opt/run.sh

# 端口号
EXPOSE 8000
EXPOSE 8001

#启动程序
CMD sh /opt/run.sh