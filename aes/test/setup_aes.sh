#!/bin/bash

ROOT= $HOME/aes

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

echo "AES setup complete"
