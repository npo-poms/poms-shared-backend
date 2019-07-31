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

## Update the configuration
The ES mapping can be changed in the files under:
`./*-es-mapping/src/main/resources/es*/mapping/*.json`

you can test your changes by executing the UnitTest in `ESMediaRepositoryPart1ITest`

### How to update index
The script will create a new index already mapped for writing (apimedia alias)
Follow the instruction in the output to manually copy the old index into the new one
and flag the new index for reading.

Connect to elastic search and find out what's the highest index.
Then run the command to generate a new index with an incremented version number:
`./push-es-media-mapping.sh localhost:9200 old_plus_1`

To be able to change the dev/test or even prod environment
you need to have a tunneling from your machine (ask to get a copy of the .ssh/config)

There you will find all the servers (any node in the cluster will do)
Tunneling via poms-test machine will allow you to reach dev and test only.

## Deploy
Once you push jenkins will build your changes.

### Jenkins Details
1. Ensure the job [Maven project POMS Shared Backend Development](https://jenkins.vpro.nl/view/POMS/job/POMS%20Shared%20Backend%20Development/)
 is completed.
2. Ensure the job [Maven project API NPO v1 Development](https://jenkins.vpro.nl/view/POMS/job/API%20NPO%20v1%20Development/)
is completed.
3. Execute the task [Deploy naar dev (rs-dev.poms.omroep.nl v1)](https://jenkins.vpro.nl/view/POMS/job/API%20NPO%20v1%20Development/batchTasks/)
to push the changes to Frontend (readonly) API in dev. 

Ensure you publish the [npo-publish](https://subversion.vpro.nl/reponl/publiekeomroep/npo-publish/trunk) (writer) 
and [npo api](https://subversion.vpro.nl/repo/nl/vpro/api/trunk) (reader)

You will have to manually update the ES index with 
the new mapping configuration. (see  [How to update index] section)

## Testing
At the moment the test is a manual changes in the gui that get published
and can be retrieved in the Frontend (Read) API.



