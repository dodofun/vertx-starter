#!/bin/bash
docker pull registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
docker run -d -p 8000:8000 -p 8001:8001 registry.cn-beijing.aliyuncs.com/dodo-fun/vertx:latest
