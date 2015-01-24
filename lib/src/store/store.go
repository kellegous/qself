package store

import (
	"compress/gzip"
	"encoding/binary"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"time"
)

// TODO(knorton): we do have one issue with the way this works. If we fail to write
// a full 11 bytes for a record, we will corrupt the stream afterward, which is quite
// terrible. Solutions are to copy the existing file 11 bytes at a time throwing out
// any non-aligning tail. Another option is to write verification points into the
// stream.

type UploadOp int

const (
	UploadRemove UploadOp = iota
	UploadKeep
)

type Config struct {
	Dir         string
	NeedsUpload func(filename string) UploadOp

	// TODO(knorton): Error callback
}

type Writer struct {
	cfg *Config
	w   io.WriteCloser
	c   io.Closer
	s   string

	ch chan func() error
}

func (w *Writer) Write(t time.Time, rr uint16) {
	w.ch <- func() error {
		if err := w.openFor(t); err != nil {
			return err
		}

		if err := binary.Write(w.w, binary.LittleEndian, t.UnixNano()); err != nil {
			return err
		}

		return binary.Write(w.w, binary.LittleEndian, rr)
	}
}

func upload(cfg *Config, filename string) {
	if cfg.NeedsUpload == nil {
		return
	}

	switch cfg.NeedsUpload(filename) {
	case UploadRemove:
		// TODO(knorton): What about errors?
		os.Remove(filename)
	}
}

func filenameFor(cfg *Config, ts string) string {
	return filepath.Join(cfg.Dir, fmt.Sprintf("%s.gz", ts))
}

func (w *Writer) openFor(t time.Time) error {
	ts := stampFor(t)

	// is this time already open?
	if ts == w.s {
		return nil
	}

	if err := w.close(); err != nil {
		return err
	}

	go upload(w.cfg, filenameFor(w.cfg, w.s))

	f, err := os.OpenFile(
		filenameFor(w.cfg, ts),
		os.O_CREATE|os.O_APPEND|os.O_RDWR,
		0666)
	if err != nil {
		return err
	}

	w.w = gzip.NewWriter(f)
	w.c = f
	w.s = ts
	return nil
}

func (w *Writer) close() error {
	var ew error
	if w.w != nil {
		ew = w.w.Close()
		w.w = nil
	}

	var ec error
	if w.c != nil {
		ec = w.c.Close()
		w.c = nil
	}

	if ew != nil {
		return ew
	} else if ec != nil {
		return ec
	}

	return nil
}

func Start(cfg *Config) (*Writer, error) {
	// ensure we have a directory
	if _, err := os.Stat(cfg.Dir); err != nil {
		if err := os.MkdirAll(cfg.Dir, 0777); err != nil {
			return nil, err
		}
	}

	w := &Writer{
		cfg: cfg,
		ch:  make(chan func() error, 10),
	}

	if err := w.openFor(time.Now()); err != nil {
		return nil, err
	}

	go func() {
		for {
			f := <-w.ch
			if err := f(); err != nil {
				log.Println(err)
			}
		}
	}()

	// return the Writer
	return w, nil
}

func stampFor(t time.Time) string {
	return t.Format("20060102")
}
