#!/usr/bin/env bash

es=http://localhost:9200
push-es-media-mapping.sh $es 0
push-es-pages-mapping.sh $es 0
push-es-pageupdates-mapping.sh $es 0

