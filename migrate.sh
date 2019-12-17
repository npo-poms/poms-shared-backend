#!/usr/bin/env bash

source="http://pomz11aas:9200"
dest="localhost:9215"
only=".*"
while getopts ":s:d:o:" opt; do
  case ${opt} in
    s )
      source=$OPTARG
      echo source: $source
      ;;
    d )
      dest=$OPTARG
      ;;
    o )
      only="^$OPTARG\$"
      ;;
    \? ) echo "Usage: $0 [-s <source>]  [-d <destination>] [-o <regex>]"
      exit
      ;;
   esac
done
shift $((OPTIND -1))
if [[ $# -ge 1 ]] ; then
 echo unrecognized argument
 exit 1;
fi

function post {
   echo $dest $1
   curl -X POST "$dest/_reindex?wait_for_completion=true" -H 'Content-Type: application/json' -d"$1"
}


function reindex {
   [[ "$1" =~ $only ]] || return

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
   post "$command"
}

function reindexType {
   [[ "$1" =~ $only ]] || [[ "$2" =~ $only ]] || return
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
  post "$command"
}


function reindexCues {
   [[ "$1" =~ $only ]] || return
   command=$( jq  -n -R \
                  --arg source "$source" \
                  --arg language "$1" \
                  --arg destIndex "subtitles_$1" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apimedia",
    type: "cue",
    query: { term: { language: { value: $language }}},
    size: 100

  },
  dest: {
    index: $destIndex,
    type: "_doc"
  }
}')
  post "$command"

}

function reindexRefs {
   [[ "$1" =~ $only ]] || return

   script="ctx._source.objectType = '$2'; ctx._id = ctx._id + '/$2';";
   command=$( jq  -n -R \
                  --arg refType "$1" \
                  --arg source "$source" \
                  --arg script "$script" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apimedia",
    type: $refType,
    size: 100

  },
  dest: {
    index: "apimedia_refs",
    type: "_doc"
  },
  script: {
     source: $script
  }
}')
   post "$command"
}

function reindexPages {
   [[ "apipages" =~ $only ]] || return

    command=$( jq -n \
                  --arg source "$source" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apipages"
  },
  dest: {
    index: "apipages",
    type: "_doc"
  },
  script: {
     source: "ctx._source.workflow = ctx._type == \"page\" ? \"PUBLISHED\" : \"DELETED\""
  }
}')
   post "$command"
}


reindex "pageupdates-publish"
reindexPages
reindex "apiqueries"
reindexType "apimedia" "program" "apimedia"
reindexType "apimedia" "group" "apimedia"
reindexType "apimedia" "segment" "apimedia"
reindexType "apimedia" "deletedprogram" "apimedia"
reindexType "apimedia" "deletedgroup" "apimedia"
reindexType "apimedia" "deletedsegment" "apimedia"
reindexCues "ar"
reindexCues "nl"
reindexCues "en"
reindexRefs "episodeRef" "episodeRef"
reindexRefs "programMemberRef" "memberRef"
reindexRefs "groupMemberRef" "memberRef"
reindexRefs "segmentMemberRef" "memberRef"
