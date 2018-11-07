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
#for j in wordnet_movie_106613686 wordnet_organization_108008335 wordnet_urban_area_108675967 wordnet_scientist_110560637
#for j in wordnet_organization_108008335
#for j in film.film organization.organization location.citytown education.academic
#for j in Film Scientist Organisation PopulatedPlace 
#for j in Organisation
#for j in Film
#for j in AdultActor
#do
     #for minsup in 100 95 90 85 80 75 70 65 60 55 50 45 40 35 30 25 20 15 10 5
     #for minsup in 75 70 65 60 55 50 45 40 35 30 25 20 15 10 5
     #for minsup in 60 30 10
     #for minsup in 100 95 90 85 80
     #for minsup in 100
     #for minsup in 40
     # do
        let "minitem = 1"
	#dataset="DBpedia/3.6/all"
        #dataset="DBpedia/2014/all"
        #dataset="DBpedia/2015-04/all"
	#dataset="DBpedia/2016-04/all"
	dataset="dbpedia/2016-10"


        #dataset="Freebase"
        #dataset="YAGO"
        #algo="apriori-mac"
        #algo="fpgrowth-mac"
        #algo="apriori-linux"
        algo="fpgrowth-linux"
        mkdir -p /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/schemas
        #fpm/${algo} -tm -s$minsup -m$minitem -Ac -g -v \(%s\) itemsets/${dataset}/${j}/transactions.txt itemsets/${dataset}/${j}/schemas/schema_minsup${minsup}.txt
        /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/fpm/${algo} -tm -s$minsup -m$minitem -g -v \(%s\) /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/transactions.txt /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/${dataset}/${j}/schemas/schema_minsup${minsup}.txt
	chmod -R 777 /etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/*
       #done 
#done

printf 'Elapsed time: %s\n' $(timer $tmr)
