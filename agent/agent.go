package main

import (
	"flag"
	"io"
	"log"
	"net"
	"net/http"
)

const (
	cmdRst byte = 0xff
	cmdHrt byte = 0x00
	cmdTmp byte = 0x01
)

func valueFrom(buf []byte) float32 {
	a, b := uint16(buf[0]), uint16(buf[1])
	return float32(a<<8|b) / 100.0
}

func serviceStream(con net.Conn) {
	defer con.Close()

	var buf [3]byte
	for {
		if _, err := io.ReadFull(con, buf[:]); err != nil {
			log.Println(err)
			return
		}

		switch buf[0] {
		case cmdRst:
			log.Printf("RST: %v", buf[1:])
		case cmdHrt:
			log.Printf("HRT: %0.2f", valueFrom(buf[1:]))
		case cmdTmp:
			log.Printf("TMP: %0.2f", valueFrom(buf[1:]))
		default:
			log.Panicf("invalid command: %d", buf[0])
		}
	}
}

func startSensorStream(addr string) error {
	adr, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		return err
	}

	lst, err := net.ListenTCP("tcp", adr)
	if err != nil {
		return err
	}

	go func() {
		for {
			con, err := lst.Accept()
			if err != nil {
				log.Print(err)
				continue
			}

			go serviceStream(con)
		}
	}()

	return nil
}

func main() {
	flagHttpAddr := flag.String("http", ":8077", "")
	flagAgntAddr := flag.String("agnt", ":8078", "")

	flag.Parse()

	if err := startSensorStream(*flagAgntAddr); err != nil {
		log.Panic(err)
	}

	if err := http.ListenAndServe(*flagHttpAddr, nil); err != nil {
		log.Panic(err)
	}
}
