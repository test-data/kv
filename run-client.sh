
HOSTS="localhost:3998"

if [ $# -ge 1 ]
  then
    HOSTS=$1
fi

java -cp target/kvdistr-1.0-SNAPSHOT.jar:docker/lib/netty-all-4.1.34.Final.jar:docker/lib/protobuf-java-3.20.1.jar:docker/lib/commons-cli-1.5.0.jar ru.kv.client.ClientCli -s $HOSTS