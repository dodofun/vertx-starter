docker rm `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `
docker rmi `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `
