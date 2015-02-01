package main

import (
	"flag"
	"fmt"
	"github.com/tarm/goserial"
	"io"
	"log"
	"net"
	"os"
	"path/filepath"
	"time"
)

const (
	cmdRst byte = 0xff
	cmdHrt byte = 0x00
	cmdTmp byte = 0x01
)

func findDevice(name string) string {
	if name != "" {
		return fmt.Sprintf("/dev/%s", name)
	}

	opts, err := filepath.Glob("/dev/tty.usbmodem*")
	if err != nil || len(opts) == 0 {
		fmt.Fprintln(os.Stderr, "unable to find a suitable usb device.")
		os.Exit(1)
	}

	if len(opts) > 1 {
		fmt.Fprintln(os.Stderr, "multiple usb devices, use --dev to specify one.\n")
		for _, opt := range opts {
			fmt.Fprintf(os.Stderr, "  %s\n", filepath.Base(opt))
		}
		os.Exit(1)
	}

	return opts[0]
}

func readUntilReset(rw io.ReadWriteCloser) error {
	var b [3]byte
	var i int

	for {
		if _, err := rw.Read(b[i : i+1]); err != nil {
			return err
		}

		i = (i + 1) % 3

		if b[0] == 0xff && b[1] == 0xff && b[2] == 0xff {
			return nil
		}
	}
}

func BackoffAfter(n int) {
	if n < 3 {
		time.Sleep(200 * time.Millisecond)
	} else if n < 10 {
		time.Sleep(1 * time.Second)
	} else {
		time.Sleep(5 * time.Second)
	}
}

func ConnectToAgent(addr string) (net.Conn, error) {
	a, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		return nil, err
	}

	n := 0
	for {
		c, err := net.DialTCP("tcp", nil, a)
		if err == nil {
			return c, nil
		}

		BackoffAfter(n)
		n++
	}
}

func ConnectToSerial(cfg *serial.Config) (io.ReadWriteCloser, error) {
	n := 0
	for {
		rw, err := serial.OpenPort(cfg)
		if err == nil {
			if err := readUntilReset(rw); err == nil {
				return rw, nil
			}
		}

		BackoffAfter(n)
		n++
	}
}

func Proxy(r io.ReadWriteCloser, w net.Conn) error {
	defer r.Close()
	defer w.Close()

	var buf [3]byte
	for {
		if _, err := io.ReadFull(r, buf[:]); err != nil {
			return err
		}

		if _, err := w.Write(buf[:]); err != nil {
			return err
		}
	}
}

func main() {
	flagDev := flag.String("dev", "", "device")
	flagAddr := flag.String("addr", "localhost:8078", "address")

	flag.Parse()

	cfg := serial.Config{
		Name: findDevice(*flagDev),
		Baud: 9600,
	}

	for {
		r, err := ConnectToSerial(&cfg)
		if err != nil {
			log.Panic(err)
		}

		w, err := ConnectToAgent(*flagAddr)
		if err != nil {
			log.Panic(err)
		}

		log.Println("Stream Established")
		Proxy(r, w)
	}
}
