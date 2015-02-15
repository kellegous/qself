BINS=bin/qagent bin/qsensor

ALL: $(BINS)

bin/qagent: $(wildcard src/**/*)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qagent/agent.go src/qagent/conns.go

bin/qsensor: $(wildcard src/**/*)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qsensor/host.go

bin/qinstall: $(wildcard src/**/*)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qinstall/install.go

bin/go-bindata:
	GOPATH=`pwd`/dep go get -u github.com/jteeuwen/go-bindata/...

bin/qmaker: $(wildcard src/**/*)
	go build -o $@ src/qmaker/maker.go

clean:
	rm -f $(BINS)