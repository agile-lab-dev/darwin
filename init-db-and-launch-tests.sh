#!/bin/bash

RET=1
while [[ RET -ne 0 ]]; do
    echo "=> Waiting for confirmation of MariaDB service startup"
    sleep 5
    mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "status" > /dev/null 2>&1
    RET=$?
done

echo "Create database and table";

mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS mysqldb;"
mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "USE mysqldb; CREATE TABLE \`SCHEMA_REPOSITORY\` (\`id\` bigint(20) DEFAULT NULL, \`schema\` text NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8;"

echo "Launching darwin-mysql-connector tests"

sbt darwin-mysql-connector/test