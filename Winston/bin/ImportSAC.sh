#!/bin/sh

PWD=`pwd`
ARGS=''

for arg in "$@" ; do
        ARGS="$ARGS $PWD/$1"
        shift
done

cd `dirname $0`/..
java -cp lib/winston.jar gov.usgs.winston.in.ImportSAC $ARGS