#!/usr/bin/env bash
#set -x

case "$1" in

    vpro_test)
        desthost=http://vs-elsearch-01.vpro.nl:9200
        ;;
    npo_dev)
        desthost=http://es-dev.pages.omroep.nl
        ;;
    npo_test)
        desthost=http://es-test.pages.omroep.nl
        ;;
    npo_prod)
        #desthost=http://es.pages.omroep.nl
        desthost=http://localhost:9208
        ;;
    localhost)
        desthost="http://localhost:9200"
        ;;
    *)
        echo "Unknown destination. Supposing that $1 is the URL."
        desthost=$1
        ;;
esac

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

if [ $# -lt 1 ];
then
    echo "Usage $0 vpro_test|npo_dev|npo_test|npo_prod|localhost|<es-url> [<index number>]"
    echo "index name:  Number of the new index to create (e.g. 2 in apipages-2)"
    exit
fi


if [ "$2" == "" ] ; then
    echo "No index number found, trying to put mappings over existing ones (supposing they are compatible)"
    destindex=pageupdates
else
    previndex=pageupdates-$(($2-1))
    destindex=pageupdates-$2
fi


targetfile=target/$destindex.json
mkdir target

echo "putting settings"
echo '{ "settings":' > $targetfile
cat $basedir/es5/setting/pageupdates.json >> $targetfile
echo ',"mappings": {' >> $targetfile

declare -a arr=( "pageupdate" "deletedpageupdate" )
for i in "${!arr[@]}"
do
    mapping=${arr[$i]}
    if [ $i -gt 0 ]; then
      echo "," >> $targetfile
    fi
    echo '"'$mapping'": ' >>  $targetfile
    cat $basedir/es5/mapping/$mapping.json >> $targetfile
done
echo -e '}\n}' >> $targetfile

echo Created $targetfile

curl -XPUT $desthost/$destindex -d@$targetfile



   echo "moving alias $previndex $destindex"

   publishalias="{
    \"actions\": [";


  if [ "$previndex" != "" -a $(($2-1)) -gt 0 ] ; then
   publishalias="$publishalias
        { \"remove\": {
            \"alias\": \"pageupdates-publish\",
            \"index\": \"$previndex\"
        }},";
  fi
   publishalias="$publishalias

        { \"add\": {
            \"alias\": \"pageupdates-publish\",
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
curl -XPOST $desthost/_reindex -d "'$reindex'"
