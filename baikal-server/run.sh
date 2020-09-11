#!/bin/sh

# baikal-server

mkdir -p /var/log/baikal-server
export JAVA_HOME=/usr/local/jdk8

. /etc/rc.d/init.d/functions
. /etc/sysconfig/network
[ "${NETWORKING}" = "no" ] && exit 0

currentdir='/usr/local/baikal-server'
JAVA_OPTS="-XX:PermSize=512M -XX:MaxPermSize=512M -Xmx2024M -Xms2024M -Xss256k -XX:ParallelGCThreads=10 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Djava.security.egd=file:/dev/./urandom -XX:+AlwaysPreTouch"
EXEC_COMMAND="${JAVA_HOME}/bin/java $JAVA_OPTS -jar ${currentdir}/baikal-server.jar --spring.profiles.active=product"
pidfile='/tmp/baikal-server'

usage() {
   echo "$0 start|stop|restart"
   exit 1
}

[ $# -ne 1 ] && usage

start() {
    if [ -f $pidfile ]
      then
         echo $pid
         echo 'baikal-server has already started.'
         exit 1
    fi
    if [ -f ${currentdir}/baikal-server.jar ]
    then
        echo 'start baikal-server'
        nohup $EXEC_COMMAND > /var/log/baikal-server/service-start.log 2>&1 &
        echo 'end of start baikal-server'
    else
      echo 'baikal-server.jar not exists!'
    fi
    RETVAL=$?
    echo
    return $RETVAL
}

stop() {
    pid=`ps -ef | grep baikal-server.jar | grep -v grep | awk '{print $2}'`
    echo "stopping..."
    kill -9 $pid || failure
    echo "done"
    RETVAL=$?
    echo
    return $RETVAL
}

restart() {
    stop
    start
}

case $1 in
    start)
    start
    ;;
    stop)
    stop
    ;;
    restart)
    restart
    ;;
    *)
    usage
    ;;
esac