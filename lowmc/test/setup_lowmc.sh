#!/bin/bash

BLOCK=$1
KEY=$2
ROUNDS=$3
SBOXES=$4

ROOT=~/Documents/SCHOOL/THESIS/lowmc-fresco/lowmc
FILE=$ROOT/src/main/java/thesis/lowmc/LowMC.java

sed -i '' "s/private static final int BLOCK_SIZE = .*/private static final int BLOCK_SIZE = $BLOCK;/" "$FILE"
sed -i '' "s/private static final int KEY_SIZE = .*/private static final int KEY_SIZE = $KEY;/" "$FILE"
sed -i '' "s/private static final int ROUNDS = .*/private static final int ROUNDS = $ROUNDS;/" "$FILE"
sed -i '' "s/private static final int SBOXES = .*/private static final int SBOXES = $SBOXES;/" "$FILE"

rm -rf $ROOT/p1/src
rm -rf $ROOT/p2/src

cp -R $ROOT/src $ROOT/p1/
cp -R $ROOT/src $ROOT/p2/
cp $ROOT/pom.xml $ROOT/p1/
cp $ROOT/pom.xml $ROOT/p2/

cd $ROOT/p1
mvn compile

cd $ROOT/p2
mvn compile

echo "LowMC setup complete: BLOCK=$BLOCK KEY=$KEY ROUNDS=$ROUNDS SBOXES=$SBOXES"
