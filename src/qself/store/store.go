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

type UploadFreq string

const (
	UploadMinutely UploadFreq = "200601021504"
	UploadHourly   UploadFreq = "2006010215"
	UploadDaily    UploadFreq = "20060102"
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
	fr  UploadFreq

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
	log.Printf("need to upload %s", filename)
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

func move(dst, src string) error {
	if err := os.Rename(src, dst); err == nil {
		return nil
	}

	r, err := os.Open(src)
	if err != nil {
		return err
	}
	defer r.Close()

	w, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer w.Close()

	if _, err = io.Copy(w, r); err != nil {
		return err
	}

	return os.Remove(src)
}

func validateAndOpen(filename string) (*os.File, error) {
	// check to see if the target file exists
	if _, err := os.Stat(filename); err != nil {
		return os.Create(filename)
	}

	// if so, move that file to tmp
	tmp := filepath.Join(os.TempDir(), filepath.Base(filename))
	if err := move(filename, tmp); err != nil {
		return nil, err
	}
	defer os.Remove(tmp)

	// open the file from tmp
	r, err := os.Open(tmp)
	if err != nil {
		return nil, err
	}
	defer r.Close()

	// now write back to the target file
	w, err := os.Create(filename)
	if err != nil {
		return nil, err
	}

	// we're going to write things back 1 frame (10 bytes) as a time to
	// make sure we have no partial records.
	var fr [10]byte
	for {
		_, err := io.ReadFull(r, fr[:])
		if err == io.ErrUnexpectedEOF || err == io.EOF {
			return w, nil
		}

		if _, err := w.Write(fr[:]); err != nil {
			return nil, err
		}
	}
}

func (w *Writer) openFor(t time.Time) error {
	ts := stampFor(t, w.fr)

	// is this time already open?
	if ts == w.s {
		return nil
	}

	if err := w.close(); err != nil {
		return err
	}

	if w.s != "" {
		go upload(w.cfg, filenameFor(w.cfg, w.s))
	}

	f, err := validateAndOpen(filenameFor(w.cfg, ts))
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

func (w *Writer) Stop() {
	close(w.ch)
}

func Start(cfg *Config, fr UploadFreq) (*Writer, error) {
	// ensure we have a directory
	if _, err := os.Stat(cfg.Dir); err != nil {
		if err := os.MkdirAll(cfg.Dir, 0777); err != nil {
			return nil, err
		}
	}

	w := &Writer{
		cfg: cfg,
		ch:  make(chan func() error, 10),
		fr:  fr,
	}

	if err := w.openFor(time.Now()); err != nil {
		return nil, err
	}

	go func() {
		for f := range w.ch {
			if err := f(); err != nil {
				log.Println(err)
			}
		}
	}()

	// return the Writer
	return w, nil
}

func stampFor(t time.Time, fr UploadFreq) string {
	return t.Format(string(fr))
}
