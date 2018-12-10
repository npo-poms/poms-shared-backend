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


dir=${desthost//[\/]/_}
mkdir -p $basedir/got/$dir
for i in "${!arr[@]}"
do
   curl $desthost/$destindex/_mapping/${arr[i]} | jq -S ".[].mappings.${arr[i]}" > $basedir/got/$dir/${arr[i]}.json
 done



