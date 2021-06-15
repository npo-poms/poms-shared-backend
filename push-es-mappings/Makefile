


tunnel-%:
	oc -n poms-util-$* port-forward poms-forward 9250:9200 & echo $$! > $@

stop-%: tunnel-%
	kill `cat $<` && rm $<


push-test: tunnel-test
	-java -jar target/push*.jar --host=https://vpc-poms-plus-elasticsearch-test-fd4f5ost27lg6jh6kitqafomfu.eu-central-1.es.amazonaws.com:9250 --user=elasticsearch --password='Dtqw0y7yoUU6Fr!h'
	make stop-test



.PHONY: start-test

clean:
