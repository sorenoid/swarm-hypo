#!/bin/sh

SGRAM_PID=/tmp/sgram.pid

WHO="tparker@usgs.gov"
JAVA_HOME=/usr/local/java
CLASSPATH=lib/vdx.jar:contrib/mysql.jar:contrib/colt.jar:$CLASSPATH
export CLASSPATH

DIR=`dirname $0`
HOST=`hostname`


case "$1" in
'start')
        
        cd $DIR
        touch $SGRAM_PID
        while [ -f $SGRAM_PID ]; do
                echo $WHAT | mail -s "Starting sgram on $HOST" $WHO
 
                java -jar lib/subnetogram.jar &
                PID=$!
                echo $PID > $SGRAM_PID
                sleep 10
                ps -p $PID > /dev/null
                while [ "$?" = "0" ]; do 
            	    sleep 10
            	    ps -p $PID > /dev/null
            	done
        done
        ;;

'stop')
        kill `cat $SGRAM_PID`
        rm -f $SGRAM_PID
        ;;

*)
        echo "Usage: $0 { start | stop }"
        ;;
esac
exit 0