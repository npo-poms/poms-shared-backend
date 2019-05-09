# NPO ES Indexes mapping

This is a shared module with all the applications that want to read and write into the ElasticSearch.

## Getting started

You can run the elastic search locally.
-- Install elastic search
-- Install icu tokenizer plugin
`sudo ${ELASTIC_HOME}/bin/elasticsearch-plugin install analysis-icu`

## Configure your local ES

Generate ES indexes for our resources:
- Execute the scripts in this folder: `./push-es-*-mapping.sh`
eg.
`./push-es-media-mapping.sh localhost:9200 1`
(pageupdate script is not relevant for API)

If you want to update your ES index to a new version
run the same script with an incremented number, the new index will
replace the old one via an alias.
eg.
`./push-es-media-mapping.sh localhost:9200 old_plus_1`

## Update the configuration

`./*-es-mapping/src/main/resources/es*/mapping/*.json`

you can test your changes by editing the UnitTest in `ESMediaRepositoryPart1ITest`
