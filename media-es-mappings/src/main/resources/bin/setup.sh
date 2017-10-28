#!/usr/bin/env bash

if [ "$DEBUG" = "true" ] ; then
    set -x
fi

if [ $# -lt 1 ];
then
    echo "Usage $0 <es-url> [<index number>|<alias>]"
    echo "index number:  Number of the new index to create (e.g. 2 in apimedia-2). If ommited the mappings are put over the old ones (only possible if they are compatible)"
    exit
fi

desthost=$1

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

if [ "$2" == "" ] ; then
    echo "No index number found, trying to put mappings over existing ones (supposing they are compatible)"
    destindex=apimedia
else
    if [[ $2 =~ ^[0-9]+$ ]] ; then
        previndex=apimedia-$(($2-1))
        destindex=apimedia-$2
    else
        echo "No index number found, trying to put mappings over existing ones (supposing they are compatible)"
        destindex=$2
    fi

fi

echo "Echo putting $basedir to $desthost/$destindex"

rm $destindex.json
if [ "$previndex" != "" ]; then
    echo "putting settings"
    echo '{ "settings":' > $destindex.json
    cat $basedir/es5/setting/apimedia.json >> $destindex.json
fi

echo ',"mappings": {' >> $destindex.json

declare -a arr=( "group" "program" "segment" "deletedprogram" "deletedgroup" "deletedsegment" "cue" "programMemberRef" "groupMemberRef" "segmentMemberRef" "episodeRef" )

for i in "${!arr[@]}"
do
    mapping=${arr[$i]}
    if [ $i -gt 0 ]; then
      echo "," >> $destindex.json
    fi
    echo '"'$mapping'": ' >>  $destindex.json
    cat $basedir/es5/mapping/$mapping.json >> $destindex.json
done
echo -e '}\n}' >> $destindex.json

echo Created $destindex.json

curl -XPUT $desthost/$destindex -d@$destindex.json

if [ "$previndex" != "" ] ; then

   echo "moving alias $previndex $destindex"

   publishalias="{
    \"actions\": [
        { \"remove\": {
            \"alias\": \"apimedia-publish\",
            \"index\": \"$previndex\"
        }},
        { \"add\": {
            \"alias\": \"apimedia-publish\",
            \"index\": \"$destindex\"
        }}
    ]
}
"
   echo $publishalias

   curl -XPOST $desthost/_aliases -d "$publishalias"

   reindex="{
  \"source\": {
    \"index\": \"$previndex\"
    },
   \"dest\": {
    \"index\": \"$destindex\"
  }
}"
   echo
   echo curl -XPOST $desthost/_reindex -d "'$reindex'"

   alias="{
    \"actions\": [
        { \"remove\": {
            \"alias\": \"apimedia\",
            \"index\": \"$previndex\"
        }},
        { \"add\": {
            \"alias\": \"apimedia\",
            \"index\": \"$destindex\"
        }}
    ]
}
"

   echo ";"
   echo curl -XPOST $desthost/_aliases -d "'$alias'"
fi
