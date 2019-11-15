#!/usr/bin/env bash

source="http://pomz11aas:9200"
dest="localhost:9215"
while getopts ":s:d:" opt; do
  case ${opt} in
    s )
      source=$OPTARG
      echo source: $source
      ;;
    d )
      dest=$OPTARG
      ;;
    \? ) echo "Usage: $0 [-s <source>]  [-d <destination>]"
      exit
      ;;
   esac
done
#set -x
function reindex {
    command=$( jq -n \
                  --arg index "$1" \
                  --arg source "$source" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: $index
  },
  dest: {
    index: $index,
    type: "_doc"
  }
}')

echo $dest $command
curl -X POST "$dest/_reindex?wait_for_completion=false" -H 'Content-Type: application/json' -d"$command"
echo
  }

function reindexType {
  command=$( jq  -n -R \
                  --arg index "$1" \
                  --arg source "$source" \
                  --arg sourceType "$2" \
                  --arg destIndex "$3" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: $index,
    type: $sourceType,
    size: 100

  },
  dest: {
    index: $destIndex,
    type: "_doc"
  }
}')

echo $dest $command
curl -X POST "$dest/_reindex?wait_for_completion=false" -H 'Content-Type: application/json' -d"$command"
echo
 }


#reindex "pageupdates-publish"
#reindex "apipages"
#reindex "apiqueries"
#reindexType "apimedia" "program" "apimedia"
#reindexType "apimedia" "group" "apimedia"
#reindexType "apimedia" "segment" "apimedia"
reindexType "apimedia" "deletedprogram" "apimedia"
#reindexType "apimedia" "deletedgroup" "apimedia"
#reindexType "apimedia" "deletedsegment" "apimedia"
#reindex "service"
#reindexComplex "3voor12" "3voor12-update" "3voor12_updates" '{ "match_all": {}}'
#reindexComplex "3voor12" "3voor12-suggest" "3voor12_suggestions" '{ "match_all": {}}'
#reindex "cinema"
#reindexComplex "media" "program" "apimedia" '{ "range": { "scheduleEvents.start": { "gte": "1561935600000" }}}'


