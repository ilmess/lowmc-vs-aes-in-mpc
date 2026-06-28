#!/bin/bash

ROOT=/Users/ilmess/Documents/SCHOOL/THESIS/lowmc-fresco/baseline

cd $ROOT/p2
OUTPUT=$(mvn exec:java \
-Dexec.args='-i2 -s tinytables -p1:localhost:5555 -p2:localhost:6666 -in 00112233445566778899aabbccddeeff')

echo "$OUTPUT"

TIME=$(echo "$OUTPUT" | grep "Execution time:" | sed 's/.*Execution time: //' | sed 's/ ms//')

echo "$(date '+%Y-%m-%d %H:%M:%S') ONLINE P2 $TIME ms" >> $ROOT/test/online_p2_time.txt
echo "$TIME" > $ROOT/test/online_p2_latest.txt
