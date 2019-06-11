#!/usr/bin/env bash

case "$1" in

    vpro_test)
        desthost=http://vs-elsearch-01.vpro.nl:9200
        ;;
    npo_dev)
        desthost=http://es-dev.querys.omroep.nl
        ;;
    npo_test)
        desthost=http://es-test.querys.omroep.nl
        ;;
    npo_prod)
        desthost=http://es.querys.omroep.nl
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

if [ $# -lt 2 ];
then
    echo "Usage $0 vpro_test|npo_dev|npo_test|npo_prod|localhost|<es-url> <index number>"
    echo "index name:  Number of the index to update (e.g. 1 in apiqueries-1)"
    exit
fi

previndex=apiqueries-$(($2-1))
destindex=apiqueries-$2

echo "Echo putting settings to $desthost/$destindex. Reading fromr $basedir"


curl -XPUT $desthost/$destindex -d @$basedir/setting/apiqueries.json

echo "Echo putting mappings to $desthost/$destindex"

curl -XPUT $desthost/$destindex/query/_mapping -d @$basedir/mapping/query.json

echo " "

echo "moving alias"

alias="
{
    \"actions\": [
        { \"remove\": {
            \"alias\": \"apiqueries\",
            \"index\": \"$previndex\"
        }},
        { \"add\": {
            \"alias\": \"apiqueries\",
            \"index\": \"$destindex\"
        }}
    ]
}
"
echo $alias


curl -XPOST $desthost/_aliases -d "$alias"


echo "Consider: stream2es es  --source http://$desthost/$previndex --target http://$desthost/$destindex"
echo "streams2es can be found at https://github.com/elasticsearch/stream2es"
