#!/usr/bin/env bash


source /Users/michiel/github/mihxil/es-mappings/push-mappings.sh

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

put $basedir "pageupdates" "pageupdate" "deletedpageupdate"

