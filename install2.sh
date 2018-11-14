#!/bin/sh

# TO DO THE FIRST TIME ONLY: (http://www.linuxproblem.org/art_9.html)
# ssh-keygen -t rsa
# validate the default directory and let the passphrase empty
# ssh pari_p1@vlad.cnam.fr mkdir -p .ssh
# cat ~/.ssh/id_rsa.pub | ssh pari_p1@vlad.cnam.fr 'cat >> .ssh/authorized_keys'
# ssh-keygen -t rsa
# validate the default directory and let the passphrase empty
# ssh pari_p1@ie5.cnam.fr mkdir -p .ssh
# cat .ssh/id_rsa.pub | ssh pari_p1@ie5.cnam.fr 'cat >> .ssh/authorized_keys'

# the goal is to compile localy
# then to upload the jar to IE5

# get directory where this script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $DIR
echo $PWD
echo 'building...'
mvn clean compile assembly:single
echo 'end building'
echo 'sending to remote server...'
cp target/lod-cm-0.0.1-SNAPSHOT-jar-with-dependencies.jar target/lod-cmOK.jar
scp target/lod-cmOK.jar pari_p1@vlad.cnam.fr:~/
ssh pari_p1@vlad.cnam.fr
# jar is on vlad, let's copy it on IE5
scp lod-cmOK.jar pari_p1@ie5.cnam.fr:/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/
echo 'end copying'
exit
# cd /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub26
# echo $PWD
# git pull
# # git fetch origin master
# # git reset --hard origin/master
# echo 'end pulling'
# cp target/lod-cm-0.0.1-SNAPSHOT-jar-with-dependencies.jar /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/lod-cmOK.jar
# echo 'end'