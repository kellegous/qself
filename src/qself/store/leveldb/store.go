package leveldb

import (
	"bytes"
	"encoding/binary"
	"github.com/syndtr/goleveldb/leveldb"
	"github.com/syndtr/goleveldb/leveldb/opt"
	"path/filepath"
	"time"
)

type Writer struct {
	db  *leveldb.DB
	str *Store
}

type Store struct {
	ch chan func()

	hrt *Writer
	tmp *Writer
}

func (s *Store) Hrt() *Writer {
	return s.hrt
}

func (s *Store) Tmp() *Writer {
	return s.tmp
}

func write(w *Writer, t time.Time, v uint16) error {
	var kb bytes.Buffer
	if err := binary.Write(&kb, binary.LittleEndian, t.UnixNano()); err != nil {
		return err
	}

	var vb bytes.Buffer
	if err := binary.Write(&vb, binary.LittleEndian, v); err != nil {
		return err
	}

	var wo opt.WriteOptions
	return w.db.Put(kb.Bytes(), vb.Bytes(), &wo)
}

func (w *Writer) WriteSync(t time.Time, v uint16) error {
	ch := make(chan error)
	w.str.ch <- func() {
		ch <- write(w, t, v)
	}
	return <-ch
}

func (w *Writer) Write(t time.Time, v uint16) {
	w.str.ch <- func() {
		write(w, t, v)
	}
}

func service(s *Store) {
	for f := range s.ch {
		f()
	}
}

func Open(dir string) (*Store, error) {
	s := &Store{
		ch: make(chan func()),
	}

	var o opt.Options

	hd, err := leveldb.OpenFile(filepath.Join(dir, "hrt"), &o)
	if err != nil {
		return nil, err
	}

	s.hrt = &Writer{
		db:  hd,
		str: s,
	}

	td, err := leveldb.OpenFile(filepath.Join(dir, "tmp"), &o)
	if err != nil {
		return nil, err
	}

	s.tmp = &Writer{
		db:  td,
		str: s,
	}

	go service(s)

	return s, nil
}
