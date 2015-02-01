BINS=bin/qagent bin/qsensor

ALL: $(BINS)

bin/qagent: $(wildcard src/**/*)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qagent/agent.go

bin/qsensor: $(wildcard src/**/*)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qsensor/host.go

clean:
	rm -f $(BINS)