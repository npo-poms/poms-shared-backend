#!/usr/bin/env bash
REMOTE=$1

source="http://pomz11aas:9200"
dest="localhost:9215"
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

echo $command
curl -X POST "$dest/_reindex?wait_for_completion=false" -H 'Content-Type: application/json' -d"$command"
echo
  }

function reindexComplex {
  command=$( jq  -n -R \
                  --arg index "$1" \
                  --arg source "$source" \
                  --arg sourceType "$2" \
                  --arg destIndex "$3" \
                  --argjson query "$4" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: $index,
    type: $sourceType,
    query: $query
  },
  dest: {
    index: $destIndex,
    type: "_doc"
  }
}')

echo $command
curl -X POST "$dest/_reindex?wait_for_completion=false" -H 'Content-Type: application/json' -d"$command"
echo
 }


reindex "pageupdates-publish"
#reindex "apipages"
#reindex "apiqueries"

#reindex "service"
#reindexComplex "3voor12" "3voor12-update" "3voor12_updates" '{ "match_all": {}}'
#reindexComplex "3voor12" "3voor12-suggest" "3voor12_suggestions" '{ "match_all": {}}'
#reindex "cinema"
#reindexComplex "media" "program" "apimedia" '{ "range": { "scheduleEvents.start": { "gte": "1561935600000" }}}'


