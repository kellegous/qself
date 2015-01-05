package main

import (
	"flag"
	"fmt"
	"github.com/tarm/goserial"
	"io"
	"log"
	"os"
	"path/filepath"
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

func valueFrom(buf []byte) float32 {
	a, b := uint16(buf[0]), uint16(buf[1])
	return float32(a<<8|b) / 100.0
}

func run(cfg *serial.Config) error {
	rw, err := serial.OpenPort(cfg)
	if err != nil {
		return err
	}
	defer rw.Close()

	if err := readUntilReset(rw); err != nil {
		return err
	}

	var buf [3]byte

	for {
		if _, err := io.ReadFull(rw, buf[:]); err != nil {
			return err
		}

		switch buf[0] {
		case cmdRst:
			log.Println("RST: %v", buf[1:])
		case cmdHrt:
			log.Printf("HRT: %0.2f", valueFrom(buf[1:]))
		case cmdTmp:
			log.Printf("TMP: %0.2f", valueFrom(buf[1:]))
		default:
			return fmt.Errorf("invalid command: %d", buf[0])
		}
	}

	return nil
}

func main() {
	flagDev := flag.String("dev", "", "device")
	flagAddr := flag.String("addr", "", "address")

	flag.Parse()

	cfg := serial.Config{
		Name: findDevice(*flagDev),
		Baud: 9600,
	}

	if err := run(&cfg); err != nil {
		log.Panic(err)
	}
}
