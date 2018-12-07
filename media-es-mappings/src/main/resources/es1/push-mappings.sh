#!/usr/bin/env bash

if [ "$DEBUG" = "true" ] ; then
    set -x
fi

if [ $# -lt 1 ];
then
    echo "Usage $0 <es-url>"
    exit
fi

desthost=$1

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )"  && pwd )"
destindex=apimedia




declare -a arr=( "group" "program" "segment" "deletedprogram" "deletedgroup" "deletedsegment" "cue" "programMemberRef" "groupMemberRef" "segmentMemberRef" "episodeRef" )

for i in "${!arr[@]}"
do
   curl -XPUT -H'content-type: application/json' $desthost/$destindex/_mapping/${arr[i]} -d$basedir/mapping/${arr[i]}.json
done



