#!/bin/sh

dir=`pwd`

port="3998"
thread="2"
if [ $# -ge 1 ]
  then
    port=$1
fi

#echo $hosts
java -cp $dir/lib/kvdistr-1.0-SNAPSHOT.jar:$dir/lib/netty-all-4.1.34.Final.jar:$dir/lib/protobuf-java-3.20.1.jar:$dir/lib/commons-cli-1.5.0.jar ru.kv.server.KVDaemon -p $port -t $thread

#java -cp lib/kvdistr-1.0-SNAPSHOT.jar:lib/netty-all-4.1.34.Final.jar:lib/protobuf-java-3.20.1.jar:lib/commons-cli-1.5.0.jar ru.kv.server.KVDaemon -p 3998 -t 2
