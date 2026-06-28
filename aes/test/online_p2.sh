#!/bin/bash

ROOT=~/Documents/SCHOOL/THESIS/lowmc-fresco/aes

cd $ROOT/p2
OUTPUT=$(mvn exec:java \
-Dexec.args='-i2 -s tinytables -p1:localhost:1111 -p2:localhost:2222 -in 00112233445566778899aabbccddeeff')

echo "$OUTPUT"

TIME=$(echo "$OUTPUT" | grep "Execution time:" | sed 's/.*Execution time: //' | sed 's/ ms//')

echo "$(date '+%Y-%m-%d %H:%M:%S') ONLINE P2 $TIME ms" >> $ROOT/test/online_p2_time.txt
echo "$TIME" > $ROOT/test/online_p2_latest.txt
