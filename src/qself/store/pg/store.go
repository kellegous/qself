package pg

import (
	"database/sql"
	"fmt"
	_ "github.com/lib/pq"
	"time"
)

const (
	tmpTable = "qself_tmp"
	hrtTable = "qself_hrt"
)

type Writer struct {
	tbl string
	str *Store
}

type Store struct {
	db *sql.DB
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
	db := w.str.db
	q := fmt.Sprintf("INSERT INTO %s (time, value) VALUES ($1, $2)", w.tbl)
	if _, err := db.Exec(q, t, v); err != nil {
		return err
	}
	return nil
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

func ensureTable(db *sql.DB, table string) error {
	s, err := db.Prepare(fmt.Sprintf(`
  create table if not exists %s (
    time timestamp NOT NULL,
    value smallint NOT NULL,
    primary key(time)
  )`, table))
	if err != nil {
		return err
	}
	defer s.Close()

	_, err = s.Exec()
	return err
}

func newWriter(s *Store, table string) (*Writer, error) {
	if err := ensureTable(s.db, table); err != nil {
		return nil, err
	}

	return &Writer{
		tbl: table,
		str: s,
	}, nil
}

func service(s *Store) {
	for f := range s.ch {
		f()
	}
}

func Open(conStr string) (*Store, error) {
	db, err := sql.Open("postgres", conStr)
	if err != nil {
		return nil, err
	}

	s := &Store{
		db: db,
		ch: make(chan func()),
	}

	s.tmp, err = newWriter(s, tmpTable)
	if err != nil {
		return nil, err
	}

	s.hrt, err = newWriter(s, hrtTable)
	if err != nil {
		return nil, err
	}

	go service(s)

	return s, nil
}
