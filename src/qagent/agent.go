package main

import (
	"encoding/binary"
	"encoding/json"
	"flag"
	"io"
	"log"
	"net"
	"net/http"
	"time"

	"qagent/api"
	"qagent/config"
	"qagent/ctx"
)

const (
	cmdRst byte = 0xff
	cmdHrt byte = 0x00
	cmdTmp byte = 0x01
	cmdUpg byte = 0x02
)

// Fe ...
type Fe struct {
	ctx.Context
	conns Conns
}

func uintValueFrom(buf []byte) uint16 {
	return uint16(buf[0])<<8 | uint16(buf[1])
}

// ServeAdvanced ...
func ServeAdvanced(con net.Conn, fe *Fe) {
	var cmd byte
	var t int64
	var v uint16

	for {
		if err := binary.Read(con, binary.LittleEndian, &cmd); err != nil {
			log.Println(err)
			return
		}

		if err := binary.Read(con, binary.LittleEndian, &t); err != nil {
			log.Panicln(err)
			return
		}

		if err := binary.Read(con, binary.LittleEndian, &v); err != nil {
			log.Panicln(err)
			return
		}

		switch cmd {
		case cmdHrt:
			if err := fe.DidReceiveHrm(time.Unix(0, t), v); err != nil {
				log.Panic(err)
			}
		case cmdTmp:
			if err := fe.DidReceiveTmp(time.Unix(0, t), v); err != nil {
				log.Panic(err)
			}
		case cmdRst, cmdUpg:
			continue
		}
	}
}

// ServeBasic ...
func ServeBasic(id int, con net.Conn, fe *Fe) {
	defer func() {
		fe.conns.DidDisconnect(id)
		con.Close()
	}()

	fe.conns.DidConnect(id)

	var buf [3]byte
	for {
		con.SetDeadline(time.Now().Add(1 * time.Minute))
		if _, err := io.ReadFull(con, buf[:]); err != nil {
			log.Println(err)
			return
		}

		switch buf[0] {
		case cmdUpg:
			ServeAdvanced(con, fe)
			return
		case cmdRst:
			continue
		case cmdHrt:
			if err := fe.DidReceiveHrm(time.Now(), uintValueFrom(buf[1:])); err != nil {
				log.Print(err)
			}
		case cmdTmp:
			if err := fe.DidReceiveTmp(time.Now(), uintValueFrom(buf[1:])); err != nil {
				log.Print(err)
			}
		default:
			log.Printf("invalid command: %d", buf[0])
			return
		}
	}
}

// ListenForSensors ...
func ListenForSensors(addr string, fe *Fe) error {
	a, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		return err
	}

	l, err := net.ListenTCP("tcp", a)
	if err != nil {
		return err
	}

	go func() {
		id := 1
		for {
			con, err := l.Accept()
			if err != nil {
				log.Print(err)
				continue
			}

			go ServeBasic(id, con, fe)
			id++
		}
	}()

	return nil
}

func writeJSON(w http.ResponseWriter, data interface{}) error {
	w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	return json.NewEncoder(w).Encode(data)
}

// RunHTTP ...
func RunHTTP(addr string, fe *Fe) error {

	m := http.NewServeMux()

	api.Setup(m, &fe.Context)

	m.HandleFunc("/api/conns", func(w http.ResponseWriter, r *http.Request) {
		res := fe.conns.Conns()
		if err := writeJSON(w, res); err != nil {
			log.Panic(err)
		}
	})

	return http.ListenAndServe(addr, m)
}

func main() {
	flagCfg := flag.String("config", "config.yaml", "")
	flag.Parse()

	var cfg config.Config
	if err := cfg.ReadFromFile(*flagCfg); err != nil {
		log.Panic(err)
	}

	var fe Fe
	if err := ctx.Make(&fe.Context, &cfg); err != nil {
		log.Panic(err)
	}

	if err := ListenForSensors(cfg.AgentAddr, &fe); err != nil {
		log.Panic(err)
	}

	if err := RunHTTP(cfg.HTTPAddr, &fe); err != nil {
		log.Panic(err)
	}
}
