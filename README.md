# NPO ES Indexes mapping

This is a shared module with all the applications that want to read and write into the ElasticSearch.

## Getting started

You can run the elastic search locally.
-- Install elastic search
-- Install icu tokenizer plugin
`sudo ${ELASTIC_HOME}/bin/elasticsearch-plugin install analysis-icu`

## ES Console
We use **ElasticSearch Head** chrome plugin to connect to ES 

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
The ES mapping can be changed in the files under:
`./*-es-mapping/src/main/resources/es*/mapping/*.json`

you can test your changes by editing the UnitTest in `ESMediaRepositoryPart1ITest`

## Deploy
Once you push jenkins will build your changes.
Ensure you publish the [npo-publish](https://subversion.vpro.nl/reponl/publiekeomroep/npo-publish/trunk) (writer) and [npo api](https://subversion.vpro.nl/repo/nl/vpro/api/trunk) (reader)

You will have to manually update the ES index with the new mapping configuration.

### How to update index
The script will create a new index already mapped for writing (apimedia alias)
Follow the instruction in the output to manually copy the old index into the new one
and flag the new index for reading.

Connect to elastic search and find out what's the highest index.
Then run the command to generate a new index with an incremented version number:
`./push-es-media-mapping.sh localhost:9200 old_plus_1`

