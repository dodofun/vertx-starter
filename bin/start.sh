#!/bin/sh
JAVA_OPTS="-Xms512m -Xmx2048m"

./vertx stop fun.dodo.verticle.MainVerticle

./stop.sh

rm -rf ./.vertx
rm -rf ../logs/*.*

CLASSPATH=.
CLASSPATH=$CLASSPATH:../etc
for i in ../lib/*.jar
do
CLASSPATH=$CLASSPATH:$i
done

nohup ./vertx run fun.dodo.verticle.MainVerticle > ../log/sys.out 2>&1 &

sleep 1
free -m

tail -f -n 100 ../log/sys.out