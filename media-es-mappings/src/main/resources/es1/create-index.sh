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
destindex=apimedia-1


curl -XPUT -H'content-type: application/json' $desthost/$destindex -d$basedir/setting/apimedia.json


alias="{
    \"actions\": [
        { \"add\": {
            \"alias\": \"apimedia\",
            \"index\": \"$destindex\"
        }
      }
    ]
}
"

curl -XPOST $desthost/_aliases -d "'$alias'"

