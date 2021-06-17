


tunnel-%:
	oc -n poms-util-$* port-forward poms-forward 9250:9200 & echo $$! > $@

stop-%: tunnel-%
	kill `cat $<` ; rm $<


push-%: tunnel-%
	$(eval ES_PW:=$(shell oc -n poms-stack-$* rsh api-server-0 sh -c 'echo $$ELASTICSEARCH_PASSWORD'))
	$(eval ES_URL:=$(shell oc -n poms-stack-$* rsh api-server-0 sh -c 'cat /conf/application.properties' | awk -F= '$$1=="elasticSearch.unicastHosts" {print $$2}'))

	echo $(ES_PW) @ $(ES_URL)
	-java -jar target/push*.jar --host=$(ES_URL):9250 --user=elasticsearch --password='$(ES_PW)'
	make stop-$<


.PHONY: start-test start-acc start-prod

clean: stop-test stop-acc stop-prod
