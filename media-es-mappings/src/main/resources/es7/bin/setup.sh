#!/usr/bin/env bash
#curl -H 'Cache-Control: no-cache'  -s "https://raw.githubusercontent.com/mihxil/es-mappings/master/push-mappings.sh" > /tmp/push-mappings.sh
#source /tmp/push-mappings.sh
source ~/github/mihxil/es-mappings/push-mappings.sh

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
basename=apimedia
commandline_args=("$@")

if [ ${#commandline_args[@]} -gt 2 ]; then
   basename=${commandline_args[2]}
fi

put $basedir $basename "media"
put $basedir "subtitles" "cue"
