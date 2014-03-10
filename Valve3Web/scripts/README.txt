
Scripts for the Selenium-based tests.  They are not used by the web server.


To run the Selenium tests:

0 - Install TestNG support in Eclipse and set the properties (output dir and 
    testng.xml)

1 - Install Selenium Grid (SG) - download from the Selenium web site and 
    unpack in a suitable directory.

2 - Modify the following files to reflect the location in which you installed 
    SG:
    - startup-hub.sh
    - startup-rc.sh
    
3 - Run "startup-hub.sh" or otherwise start the Selenium hub.  Similarly,
    run "startup-rc.sh" to start Remote Control (RC) instances.  This script
    takes an optional numerical argument which is the number of instances to
    run.

4 - Deploy the Valve system.

5 - Modify testng.xml to reflect the deployment details and the location of
    the hub, etc.

6 - In Eclipse, right-click on gov.usgs.selenium.gps.SingleGpsPlot.java and
    select "Run as... TestNG Test".  This should run a single, simple test.


For more complex tests, you will need to repeat this deploy on several 
machines.  You can run RC instances in various places (firewalls allowing)
and modify testng.xml and the test parameters in the code appropriately.

For example, to test 20 simutaneous users, you might have 10 RC instances
on one machine and 10 on another, and then run a test with a thread pool
of size 10.

The pooling can be configured on individual tests, or by modifying testng.xml
(no more tests will run in parallel than have RC instances available). 


For compatibility tests, install an RC on Linux with Firefox, another on 
OS X with Solaris, and another on Windows with IE.  Then right click on
testng-compat.xml in Eclipse and run as a TestNG test.
