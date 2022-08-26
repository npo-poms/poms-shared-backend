


tunnel-%:
	oc -n poms-util-$* port-forward poms-forward 9250:9200 & echo $$! > $@

stop-%: tunnel-%
	kill `cat $<` ; rm $<


push-%: tunnel-% target
	$(eval ES_PW:=$(shell oc -n poms-stack-$* rsh api-server-0 sh -c 'echo $$ELASTICSEARCH_PASSWORD'))
	$(eval ES_URL:=$(shell oc -n poms-stack-$* rsh api-server-0 sh -c 'cat /conf/application.properties' | awk -F= '$$1=="elasticSearch.unicastHosts" {print $$2}'))

	echo $(ES_PW) @ $(ES_URL)
	-java -jar target/push*.jar --host=$(ES_URL):9250 --user=elasticsearch --password='$(ES_PW)'
	make stop-$*


target:
	mvn package


.PHONY: target

clean: stop-test stop-acc stop-prod

docker:
	export poms_version=6.0-SNAPSHOT
	docker run -v "$$(pwd)":/share --entrypoint "" gcr.io/kaniko-project/executor:debug   /kaniko/executor \
      --context /share \
      --dockerfile /share/Dockerfile \
      --build-arg POM_VERSION=6.0-SNAPSHOT \
      --cache=false \
      --destination test:test \
      --no-push

