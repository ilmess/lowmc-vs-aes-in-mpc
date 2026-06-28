#!/bin/bash

ROOT=~/Documents/SCHOOL/THESIS/lowmc-fresco/lowmc

cd $ROOT/p1
OUTPUT=$(mvn exec:java \
-Dexec.args='-i1 -s tinytables -p1:localhost:3333 -p2:localhost:4444 -in 000102030405060708090a0b0c0d0e0f')

echo "$OUTPUT"

TIME=$(echo "$OUTPUT" | grep "Execution time:" | sed 's/.*Execution time: //' | sed 's/ ms//')

echo "$(date '+%Y-%m-%d %H:%M:%S') ONLINE P1 $TIME ms" >> $ROOT/test/online_p1_time.txt
echo "$TIME" > $ROOT/test/online_p1_latest.txt
