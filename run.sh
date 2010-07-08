#!/bin/bash

if [ -e mbot.jar ]; then

echo "Using jar file"

    java -cp \
lib/scc.jar:\
lib/apache-cassandra-0.6.2.jar:\
lib/libthrift-r917130.jar:\
lib/log4j-1.2.14.jar:\
lib/scc.jar:\
lib/slf4j-api-1.5.8.jar:\
lib/slf4j-log4j12-1.5.8.jar:\
mbot.jar Main

else

echo "Using class files"

    java -cp \
lib/scc.jar:\
lib/apache-cassandra-0.6.2.jar:\
lib/libthrift-r917130.jar:\
lib/log4j-1.2.14.jar:\
lib/scc.jar:\
lib/slf4j-api-1.5.8.jar:\
lib/slf4j-log4j12-1.5.8.jar:\
classes/ Main

fi
