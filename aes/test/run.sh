#!/bin/bash

ROOT=~/Documents/SCHOOL/THESIS/lowmc-fresco/aes

cd $ROOT/test

./prepro_p1.sh &
./prepro_p2.sh &
wait

PREPRO_P1=$(cat prepro_p1_latest.txt)
PREPRO_P2=$(cat prepro_p2_latest.txt)

if (( $(echo "$PREPRO_P1 < $PREPRO_P2" | bc -l) )); then
  PREPRO=$PREPRO_P1
else
  PREPRO=$PREPRO_P2
fi

./online_p1.sh &
./online_p2.sh &
wait

ONLINE_P1=$(cat online_p1_latest.txt)
ONLINE_P2=$(cat online_p2_latest.txt)

if (( $(echo "$ONLINE_P1 < $ONLINE_P2" | bc -l) )); then
  ONLINE=$ONLINE_P1
else
  ONLINE=$ONLINE_P2
fi

TOTAL=$(echo "$PREPRO + $ONLINE" | bc -l)

echo "$(date '+%Y-%m-%d %H:%M:%S') PREPRO=$PREPRO ms ONLINE=$ONLINE ms TOTAL=$TOTAL ms" >> experiment_log.txt

echo ""
echo "=============================="
echo "AES RESULTS"
echo "=============================="
echo "Prepro P1:   $PREPRO_P1 ms"
echo "Prepro P2:   $PREPRO_P2 ms"
echo "Prepro used: $PREPRO ms"
echo ""
echo "Online P1:   $ONLINE_P1 ms"
echo "Online P2:   $ONLINE_P2 ms"
echo "Online used: $ONLINE ms"
echo ""
echo "TOTAL:       $TOTAL ms"
echo "=============================="
