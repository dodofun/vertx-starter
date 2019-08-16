#!/bin/bash

docker stop `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `
docker rm   `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `
docker rmi `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `

docker pull registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
docker run -d -p 8000:8000 -p 8001:8001 registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
