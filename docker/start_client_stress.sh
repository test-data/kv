#!/bin/sh

dir=`pwd`

hosts="localhost:3998"

if [ $# -ge 1 ]
  then
    hosts=$1
fi

echo $hosts
java -cp $dir/lib/kvdistr-1.0-SNAPSHOT.jar:$dir/lib/netty-all-4.1.34.Final.jar:$dir/lib/protobuf-java-3.20.1.jar:$dir/lib/commons-cli-1.5.0.jar:$dir/lib/commons-lang3-3.9.jar ru.kv.client.ClientStressTest -s $hosts -c 100000
