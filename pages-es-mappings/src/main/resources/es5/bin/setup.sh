#!/usr/bin/env bash
#set -x


basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

if [ $# -lt 1 ];
then
    echo "Usage $0 <es-url> [<index number>]"
    echo "index name:  Number of the new index to create (e.g. 2 in apipages-2)"
    exit
fi

desthost=$1
needssettings=false
if [ "$2" == "" ] ; then
    echo "No index number found, trying to put mappings over existing ones (supposing they are compatible)"
    destindex=apipages-publish
else
    previndex=apipages-$(($2-1))
    destindex=apipages-$2
fi



rm $destindex.json
echo '{' > $destindex.json
if [ "$needssettings" = true ]; then
    echo "putting settings"
    echo '"settings":' >> $destindex.json
    cat $basedir/setting/apipages.json >> $destindex.json
    echo "," >> $destindex.json

    echo '"mappings": {' >> $destindex.json

    echo 'page": ' >>  $destindex.json
    cat $basedir/mapping/page.json >> $destindex.json
    echo -e '}\n}' >> $destindex.json

    echo Created $destindex.json
    curl -XPUT -H'content-type: application/json' $desthost/$destindex -d@$destindex.json

else
    echo "previndex $previndex . No settings necessary"
    echo curl -XPUT -H'content-type: application/json' $desthost/page/_mapping -d@$basedir/mapping/page.json
    curl -XPUT -H'content-type: application/json' $desthost/$destindex/page/_mapping -d@$basedir/mapping/page.json
fi



if [ "$previndex" != "" ] ; then

   echo "moving alias $previndex $destindex"

   publishalias="{
    \"actions\": [
        { \"remove\": {
            \"alias\": \"apipages-publish\",
            \"index\": \"$previndex\"
        }},
        { \"add\": {
            \"alias\": \"apipages-publish\",
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
            \"alias\": \"apipages\",
            \"index\": \"$previndex\"
        }},
        { \"add\": {
            \"alias\": \"apipages\",
            \"index\": \"$destindex\"
        }}
    ]
}
"

   echo "Followed by"
   echo curl -XPOST $desthost/_aliases -d "'$alias'"
fi


