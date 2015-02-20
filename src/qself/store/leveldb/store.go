package leveldb

import (
	"bytes"
	"encoding/binary"
	"github.com/syndtr/goleveldb/leveldb"
	"github.com/syndtr/goleveldb/leveldb/opt"
	"time"
)

var (
	tmpPrefix = []byte("tmp")
	hrtPrefix = []byte("hrt")
)

type Writer struct {
	pfx []byte
	str *Store
}

type Store struct {
	db *leveldb.DB
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
	if _, err := kb.Write(w.pfx); err != nil {
		return err
	}

	if err := binary.Write(&kb, binary.LittleEndian, t.UnixNano()); err != nil {
		return err
	}

	var vb bytes.Buffer
	if err := binary.Write(&vb, binary.LittleEndian, v); err != nil {
		return err
	}

	db := w.str.db
	var wo opt.WriteOptions

	return db.Put(kb.Bytes(), vb.Bytes(), &wo)
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

func Open(filename string) (*Store, error) {
	var o opt.Options
	db, err := leveldb.OpenFile(filename, &o)
	if err != nil {
		return nil, err
	}

	s := &Store{
		db: db,
		ch: make(chan func()),
	}

	s.tmp = &Writer{
		pfx: tmpPrefix,
		str: s,
	}

	s.hrt = &Writer{
		pfx: hrtPrefix,
		str: s,
	}

	go service(s)

	return s, nil
}
