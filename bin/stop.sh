#!/bin/sh
APP_MAIN=fun.dodo.verticle.MainVerticle

./vertx stop $APP_MAIN
sleep 2

myPID=0

getPID(){
    javaps=`ps -ef | grep $APP_MAIN | grep -v grep`
    if [ -n "$javaps" ]; then
        myPID=`echo $javaps | awk '{print $2}'`
    else
        myPID=0
    fi
}

shutdown(){
    getPID
    if [ $myPID -ne 0 ]; then
        echo -n "Stopping $APP_MAIN(PID=$myPID)..."

        kill -9 $myPID

        if [ $? -eq 0 ]; then
            echo "[Success]"
        else
            echo "[Failed]"
        fi
    else
        echo "$APP_MAIN is not running"
    fi
}

shutdown
