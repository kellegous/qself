BINS=bin/qmaker bin/qagent bin/qsensor bin/qinstall

default: bin/qmaker

all: $(BINS)

bin/qagent: $(shell find src)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qagent/agent.go src/qagent/conns.go

bin/qsensor: $(shell find src)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qsensor/host.go

bin/qinstall: $(shell find src)
	GOPATH=`pwd`:`pwd`/dep go build -o $@ src/qinstall/install.go src/qinstall/fake.go

bin/go-bindata:
	GOPATH=`pwd`/dep go get -u github.com/jteeuwen/go-bindata/...

bin/qmaker: $(shell find src)
	go build -o $@ src/qmaker/maker.go

clean:
	rm -f $(BINS)
