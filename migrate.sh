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
   echo
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

function reindexMediaType {
   [[ "$1" =~ $only ]] || [[ "$2" =~ $only ]] || return

   read -r -d '' script << EOM
      if (ctx._source.credits != null ) {
           for (int i = 0; i < ctx._source.credits.length; ++i) {
               Map c = ctx._source.credits[i];
               String fullName = c.fullName;
               if (c.fullName == null) {
                  c.name = (c.familyName == null ? "" : c.familyName) + (c.givenName == null ? "":  ", " + c.givenName);
               }
               c.name = c.fullName;
               c.objectType = 'person';
               c.remove('fullName');
           }
      }
EOM
   command=$( jq  -n -R \
                  --arg index "$1" \
                  --arg source "$source" \
                  --arg sourceType "$2" \
                  --arg destIndex "$3" \
                  --arg script "$script" \ '
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
  },
  script: {
     source: $script,
     lang: "painless"
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

    read -r -d '' script << EOM
      if ( ctx._type != "page") {
          ctx._source.workflow =  "DELETED";
      }
      ctx._source.expandedWorkflow = ctx._type == "page" ? "PUBLISHED" : "DELETED";
EOM

    command=$( jq -n \
                  --arg source "$source" \
                  --arg script "$script" \
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
     source: $script,
     lang: "painless"

  }
}')
   post "$command"
}


reindex "pageupdates-publish"
reindexPages
reindex "apiqueries"
reindexMediaType "apimedia" "program" "apimedia"
reindexMediaType "apimedia" "group" "apimedia"
reindexMediaType "apimedia" "segment" "apimedia"
reindexMediaType "apimedia" "deletedprogram" "apimedia"
reindexMediaType "apimedia" "deletedgroup" "apimedia"
reindexMediaType "apimedia" "deletedsegment" "apimedia"
reindexCues "ar"
reindexCues "nl"
reindexCues "en"
# other language don't occur on production
reindexRefs "episodeRef" "episodeRef"
reindexRefs "programMemberRef" "memberRef"
reindexRefs "groupMemberRef" "memberRef"
reindexRefs "segmentMemberRef" "memberRef"
