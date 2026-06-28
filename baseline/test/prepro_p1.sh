#!/bin/bash

ROOT=/Users/ilmess/Documents/SCHOOL/THESIS/lowmc-fresco/baseline

cd $ROOT/p1
rm -f tinytables

OUTPUT=$(mvn exec:java \
-Dexec.args='-i1 -s tinytablesprepro -p1:localhost:5555 -p2:localhost:6666 -in 000102030405060708090a0b0c0d0e0f')

echo "$OUTPUT"

TIME=$(echo "$OUTPUT" | grep "Execution time:" | sed 's/.*Execution time: //' | sed 's/ ms//')

echo "$(date '+%Y-%m-%d %H:%M:%S') PREPRO P1 $TIME ms" >> $ROOT/test/prepro_p1_time.txt
echo "$TIME" > $ROOT/test/prepro_p1_latest.txt
