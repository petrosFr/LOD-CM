#!/bin/sh
cd /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub26
echo $PWD
git pull
# git fetch origin master
# git reset --hard origin/master
echo 'end pulling'
mvn clean compile assembly:single
echo 'end building'
cp target/lod-cm-0.0.1-SNAPSHOT-jar-with-dependencies.jar /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/lod-cmOK.jar
echo 'end'