package store

import (
	"bytes"
	"encoding/binary"
	"errors"
	"path/filepath"
	"time"

	"github.com/syndtr/goleveldb/leveldb"
	"github.com/syndtr/goleveldb/leveldb/opt"
	"github.com/syndtr/goleveldb/leveldb/util"
)

// ErrBailOut ...
var ErrBailOut = errors.New("stop")

var (
	// StartOfTime ...
	StartOfTime = time.Time{}

	// LimitOfTime ...
	LimitOfTime = time.Date(4000, 1, 1, 0, 0, 0, 0, time.Local)
)

// Collection ...
type Collection struct {
	db  *leveldb.DB
	str *Store
}

// Store ...
type Store struct {
	ch chan func()

	hrt *Collection
	tmp *Collection
}

// Hrt ...
func (s *Store) Hrt() *Collection {
	return s.hrt
}

// Tmp ...
func (s *Store) Tmp() *Collection {
	return s.tmp
}

func write(w *Collection, t time.Time, v uint16) error {
	var kb bytes.Buffer
	if err := binary.Write(&kb, binary.BigEndian, t.UnixNano()); err != nil {
		return err
	}

	var vb bytes.Buffer
	if err := binary.Write(&vb, binary.BigEndian, v); err != nil {
		return err
	}

	var wo opt.WriteOptions
	return w.db.Put(kb.Bytes(), vb.Bytes(), &wo)
}

// WriteSync ...
func (c *Collection) WriteSync(t time.Time, v uint16) error {
	ch := make(chan error)
	c.str.ch <- func() {
		ch <- write(c, t, v)
	}
	return <-ch
}

// Write ...
func (c *Collection) Write(t time.Time, v uint16) {
	c.str.ch <- func() {
		write(c, t, v)
	}
}

func dispatchTo(key, val []byte, fn func(t time.Time, v uint16) error) error {
	var t int64
	var v uint16
	var buf bytes.Buffer

	buf.Write(key)
	if err := binary.Read(&buf, binary.BigEndian, &t); err != nil {
		return err
	}

	buf.Write(val)
	if err := binary.Read(&buf, binary.BigEndian, &v); err != nil {
		return err
	}

	return fn(time.Unix(0, t), v)
}

func timeToBytes(t time.Time) []byte {
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, t.UnixNano())
	return buf.Bytes()
}

// ForEachInRange ...
func (c *Collection) ForEachInRange(start, limit time.Time, fn func(t time.Time, v uint16) error) error {
	var ro opt.ReadOptions
	it := c.db.NewIterator(&util.Range{
		Start: timeToBytes(start),
		Limit: timeToBytes(limit),
	}, &ro)
	defer it.Release()

	for it.Next() {
		if err := dispatchTo(it.Key(), it.Value(), fn); err != nil {
			if err == ErrBailOut {
				return nil
			} else if err != nil {
				return err
			}
		}
	}

	return it.Error()
}

// ForEachFromEnd ...
func (c *Collection) ForEachFromEnd(fn func(t time.Time, v uint16) error) error {
	var ro opt.ReadOptions
	it := c.db.NewIterator(nil, &ro)
	defer it.Release()

	if !it.Last() {
		return it.Error()
	}

	for {
		if err := dispatchTo(it.Key(), it.Value(), fn); err != nil {
			if err == ErrBailOut {
				return nil
			}
			return err
		}

		if !it.Prev() {
			break
		}
	}

	return it.Error()
}

// ForEach ...
func (c *Collection) ForEach(fn func(t time.Time, v uint16) error) error {
	var ro opt.ReadOptions
	it := c.db.NewIterator(nil, &ro)
	defer it.Release()

	for it.Next() {
		if err := dispatchTo(it.Key(), it.Value(), fn); err != nil {
			if err == ErrBailOut {
				return nil
			}
			return err
		}
	}

	return it.Error()
}

func service(s *Store) {
	for f := range s.ch {
		f()
	}
}

// Open ...
func Open(dir string) (*Store, error) {
	s := &Store{
		ch: make(chan func()),
	}

	var o opt.Options

	hd, err := leveldb.OpenFile(filepath.Join(dir, "hrt"), &o)
	if err != nil {
		return nil, err
	}

	s.hrt = &Collection{
		db:  hd,
		str: s,
	}

	td, err := leveldb.OpenFile(filepath.Join(dir, "tmp"), &o)
	if err != nil {
		return nil, err
	}

	s.tmp = &Collection{
		db:  td,
		str: s,
	}

	go service(s)

	return s, nil
}
