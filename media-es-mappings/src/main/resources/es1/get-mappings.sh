#!/usr/bin/env bash

if [ "$DEBUG" = "true" ] ; then
    set -x
fi

if [ $# -lt 1 ];
then
   desthost=http://localhost:9200
else
   desthost=$1
fi

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )"  && pwd )"
destindex=apimedia




declare -a arr=( "group" "program" "segment" "deletedprogram" "deletedgroup" "deletedsegment" "cue" "programMemberRef" "groupMemberRef" "segmentMemberRef" "episodeRef" )



mkdir gotmapping
echo $desthost/$destindex > gotmapping/desthost/
for i in "${!arr[@]}"
do
   curl $desthost/$destindex/_mapping/${arr[i]} | jq ".[].mappings.${arr[i]}" > $basedir/gotmapping/${arr[i]}.json
 done



