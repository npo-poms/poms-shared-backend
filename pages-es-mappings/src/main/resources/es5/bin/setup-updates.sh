#!/usr/bin/env bash

curl -s "https://raw.githubusercontent.com/mihxil/es-mappings/master/push-mappings.sh" > /tmp/push-mappings.sh
source /tmp/push-mappings.sh

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

put $basedir "pageupdates" "pageupdate" "deletedpageupdate"

