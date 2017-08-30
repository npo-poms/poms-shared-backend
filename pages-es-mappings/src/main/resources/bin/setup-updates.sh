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



rm $destindex.json
echo "putting settings"
echo '{ "settings":' > $destindex.json
cat $basedir/setting/pageupdates.json >> $destindex.json
echo ',"mappings": {' >> $destindex.json

declare -a arr=( "pageupdate" "deletedpageupdate" )
for i in "${!arr[@]}"
do
    mapping=${arr[$i]}
    if [ $i -gt 0 ]; then
      echo "," >> $destindex.json
    fi
    echo '"'$mapping'": ' >>  $destindex.json
    cat $basedir/mapping/$mapping.json >> $destindex.json
fi
echo -e '}\n}' >> $destindex.json

echo Created $destindex.json

curl -XPUT $desthost/$destindex -d@$destindex.json

