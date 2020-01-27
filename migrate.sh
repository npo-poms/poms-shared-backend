#!/usr/bin/env bash
source="http://pomz11aas:9200"
dest="localhost:9215"
only=".*"
since=0
while getopts ":s:d:o:f:" opt; do
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
    f )
      since="$OPTARG"
      ;;
    \? ) echo "Usage: $0 [-s <source>]  [-d <destination>] [-o <regex>] -f <since time stamp min millis since 1970>"
      echo "for test:  ./migrate.sh -s http://poms11aas:9200 -d http://localhost:9221"
      echo "for production:  ./migrate.sh -s http://poms11aas:9200 -d http://localhost:9221"

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
   echo curl -X POST "$dest/_reindex?wait_for_completion=true" -H 'Content-Type: application/json' -d"$1"
   time curl -X POST "$dest/_reindex?wait_for_completion=true" -H 'Content-Type: application/json' -d"$1"
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
   [[ "apimedia" =~ $only ]] || [[ "$1" =~ $only ]] || return

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
                  --arg source "$source" \
                  --arg script "$script" \
                  --arg sourceType "$1" \
                  --arg since "$since" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apimedia",
    type: $sourceType,
    size: 100,
    query: {
      range: {
        publishDate: {
           gte: $since
        }
      }
    }
  },
  dest: {
    index: "apimedia",
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
   if [[ "$since" !=  "0" ]] ; then
      echo "Since not supported for cues. Reindexing them all"
   fi
   command=$( jq  -n -R \
                  --arg source "$source" \
                  --arg language "$1" \
                  --arg destIndex "subtitles_$1" \
                   --arg since "$since" \
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
                  --arg since "$since" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apimedia",
    type: $refType,
    size: 100,
    query: {
      range: {
        added: {
           gte: $since
        }
      }
    }

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
                  --arg since "$since" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "apipages",
    query: {
      range: {
        lastPublished: {
           gte: $since
        }
      }
    }
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


function reindexPageUpdates {
   [[ "pageupdates-publish" =~ $only ]] || return

    command=$( jq -n \
                  --arg index "$1" \
                  --arg source "$source" \
                  --arg since "$since" \
                  '
 { source: {
    remote: {
      host: $source
    },
    index: "pageupdates-publish",
    query: {
      range: {
        lastPublished: {
           gte: $since
        }
      }
    }
  },
  dest: {
    index: "pageupdates-publish",
    type: "_doc"
  }
}')
   post "$command"
}

timeinmillis=$(( `date +%s` * 1000 ))
echo "Current time in millisecond: $timeinmillis. Use this as an argument for a subsequent run (if the publisher is currently still running)"
echo $0 -s $source -d $dest -f $timeinmillis

reindexPageUpdates
reindexPages
reindex "apiqueries"
reindexMediaType "program"
reindexMediaType  "group"
reindexMediaType "segment"
reindexMediaType "deletedprogram"
reindexMediaType "deletedgroup"
reindexMediaType "deletedsegment"
reindexCues "ar"
reindexCues "nl"
reindexCues "en"
# other language don't occur on production
reindexRefs "episodeRef" "episodeRef"
reindexRefs "programMemberRef" "memberRef"
reindexRefs "groupMemberRef" "memberRef"
reindexRefs "segmentMemberRef" "memberRef"

echo next run might be:
echo $0 -s $source -d $dest -f $timeinmillis

