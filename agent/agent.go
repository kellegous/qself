package main

import (
	"flag"
	"heart"
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

func uintValueFrom(buf []byte) uint16 {
	return uint16(buf[0])<<8 | uint16(buf[1])
}

func serviceStream(con net.Conn) {
	hs := heart.NewStats(16)

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
			hs.AddInterval(uintValueFrom(buf[1:]))
			hr := hs.Hr()
			if hr > 0.0 {
				log.Printf("HRT: %0.2f bpm", hr)
			}
		case cmdTmp:
			log.Printf("TMP: %0.2f deg", float32(uintValueFrom(buf[1:]))/100.0)
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
