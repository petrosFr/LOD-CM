#!/bin/bash
# timer function
function timer()
{
    if [[ $# -eq 0 ]]; then
        echo $(date '+%s')
    else
        local  stime=$1
        etime=$(date '+%s')

        if [[ -z "$stime" ]]; then stime=$etime; fi

        dt=$((etime - stime))
        ds=$((dt % 60))
        dm=$(((dt / 60) % 60))
        dh=$((dt / 3600))
        printf '%d:%02d:%02d' $dh $dm $ds
    fi
}

j=$1
minsup=$2


# Mine frequent itemsets

tmr=$(timer)

# Mine maximal frequent itemsets (fpgrowth or apriori)

let "minitem = 1"

dataset="dbpedia/2016-10"

algo="fpgrowth-linux"
mkdir -p /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/schemas

/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/fpm/${algo} -tm -s$minsup -m$minitem -g -v \(%s\) /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/transactions.txt /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/schemas/schema_minsup${minsup}.txt

chmod -R 777 /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/*


printf 'Elapsed time: %s\n' $(timer $tmr)
