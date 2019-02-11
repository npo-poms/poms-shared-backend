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
destindex=apipages-1


curl -XPUT -H'content-type: application/json' $desthost/$destindex -d@$basedir/setting/apipages.json


alias="{
    \"actions\": [
        { \"add\": {
            \"alias\": \"apipages\",
            \"index\": \"$destindex\"
        }
      }
    ]
}
"

curl -XPOST $desthost/_aliases -d "$alias"

