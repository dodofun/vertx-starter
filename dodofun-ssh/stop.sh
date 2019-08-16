#!/bin/bash
docker stop `docker ps -a| grep registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest | awk '{print $1}' `
