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

type UploadOp int

const (
	UploadRemove UploadOp = iota
	UploadKeep   UploadOp
)

type Config struct {
	Dir         string
	NeedsUpload func(filename string) UploadOp

	// TODO(knorton): Error callback
}

// TODO(knorton): Here is what this will do.
//  (1) write each sample to a local file w/ timestamp
//  (2) at predefined times, it will push the file to GCS(?)
//  (3) in case of error, it will need to reach out to me in
//      some way. this will probably just be a callback here.
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

	f, err := os.OpenFile(filenameFor(w.cfg, ts), os.O_RDWR|os.O_APPEND, os.ModePerm)
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
		if err := os.MkdirAll(cfg.Dir, os.ModePerm); err != nil {
			return err
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
	return w, err
}

func stampFor(t time.Time) string {
	return t.Format("20060102")
}
