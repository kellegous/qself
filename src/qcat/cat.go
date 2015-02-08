package main

import (
	"bufio"
	"bytes"
	"flag"
	"fmt"
	"github.com/tarm/goserial"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
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
		fmt.Fprintln(os.Stderr, "multiple usb devices, use --device to specify one.")
		for _, opt := range opts {
			fmt.Fprintf(os.Stderr, "    %s\n", filepath.Base(opt))
		}
		os.Exit(1)
	}

	return opts[0]
}

type record struct {
	t time.Time
	s string
}

func (r *record) String() string {
	return fmt.Sprintf("%s: %s", r.t.Format(time.RFC3339), r.s)
}

type logger struct {
	buf []*record
	idx int
	lck sync.RWMutex
}

func (l *logger) GetLogs() []string {
	l.lck.RLock()
	defer l.lck.RUnlock()

	n, c := len(l.buf), cap(l.buf)
	r := make([]string, n)
	for i := 0; i < n; i++ {
		r[i] = l.buf[(l.idx+i)%c].String()
	}
	return r
}

func (l *logger) append(s string) {
	l.lck.Lock()
	defer l.lck.Unlock()

	r := &record{
		t: time.Now(),
		s: s,
	}

	if len(l.buf) < cap(l.buf) {
		l.buf = append(l.buf, r)
	} else {
		l.buf[l.idx] = r
		l.idx = (l.idx + 1) % cap(l.buf)
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

func (l *logger) connect(cfg *serial.Config) error {
	rw, err := serial.OpenPort(cfg)
	if err != nil {
		return err
	}
	defer rw.Close()

	var buf bytes.Buffer
	br := bufio.NewReader(rw)

	for {
		b, p, err := br.ReadLine()
		if err != nil {
			return err
		}

		buf.Write(b)
		if p {
			continue
		}

		l.append(buf.String())

		buf.Reset()
	}
}

func startLogger(cfg *serial.Config, n int) (*logger, error) {
	l := logger{
		buf: make([]*record, 0, n),
	}

	go func() {
		n := 0
		for {
			if err := l.connect(cfg); err != nil {
				log.Print(err)
				n++
			} else {
				n = 0
			}

			BackoffAfter(n)
		}
	}()

	return &l, nil
}

func main() {
	flagAddr := flag.String("addr", ":8070", "")
	flagDevc := flag.String("device", "", "")
	flag.Parse()

	cfg := serial.Config{
		Name: findDevice(*flagDevc),
		Baud: 9600,
	}

	l, err := startLogger(&cfg, 100)
	if err != nil {
		log.Panic(err)
	}

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintln(w, strings.Join(l.GetLogs(), "\n"))
	})

	if err := http.ListenAndServe(*flagAddr, nil); err != nil {
		log.Panic(err)
	}
}
