@echo off
rem = A simple batch file to set the Java CLASSPATH.  Does not include Valve or Valve contributed .jar files.
rem = cp [path to root]
rem =

set CLASSPATH=%1\Pinnacle;%1\Data;%1\Earthworm\;%1\Math;%1\Net;%1\Plot;%1\Swarm;%1\Util;%1\Winston;%1\USGS\contrib\mysql.jar;%1\USGS\contrib\colt.jar;%1\USGS\contrib\seed-pdcc.jar;%1\VDX
