## PoC key/value Distributed store 

Задача - создание прототипа хранилища ключ/значение.

## Принцип работы
Идея первоначальная была в созднии мастера с Raft и обеспечением консистентности/репликации/репарционирования. Но т.к репартичионирование делать не нужно, репликация тоже не нужна. И вообще в такой задаче вылет любой ноды - это потеря данных. Проще перенести логику распределения данных на клиента (слой middleware, при этом обеспечив равномерный консистентный хэш). Для сериализации и десериализации используется protobuf. Есть класс KVDaemon - который используется и на клиенте и на серверной части. Для отправки автоматом пар ключ/значение можно использовать класс - ClientStressTest

## Сборка:
mvn clean install 

## Запуск сервера
java -cp target/kvdistr-1.0-SNAPSHOT.jar:lib/netty-all-4.1.34.Final.jar:lib/protobuf-java-3.20.1.jar:lib/commons-cli-1.5.0.jar ru.kv.server.KVDaemon -p 3999 -t 2
где p - указание порта, t - кол-во потоков

Запуск можно сделать скриптом - run-server.sh Х, где Х размер кластера

## Запуск клиента 
java -cp target/kvdistr-1.0-SNAPSHOT.jar:lib/netty-all-4.1.34.Final.jar:lib/protobuf-java-3.20.1.jar:lib/commons-cli-1.5.0.jar ru.kv.client.ClientCli -s localhost:3998
s - указание всех серверов, например "-s localhost:3998,localhost:3997,localhost:3996"

Запуск можно сделать скриптом - run-client.sh Х, где список серверов "localhost:3998,localhost:3997,localhost:3996""

## Docker
сборка image
 docker build -t kv . 

запуск docker-compose - запускает 3 сервера (сеть host протестировать не удалось из-за ограничений OS, можно подключиться в любой и запустить клиента скриптом start_client.sh)
 docker-compose up 

## HASH
Испольузется hash на клиенте, где передаем все сервера с key/value - jumpConsistentHash 
почитать можно тут - http://arxiv.org/ftp/arxiv/papers/1406/1406.2294.pdf

