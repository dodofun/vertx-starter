#!/bin/bash
# docker login --username=dodofun2020 registry.cn-beijing.aliyuncs.com
docker tag dodofun/vertx:latest registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
docker push registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest

# docker pull registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest