package store

import (
	"time"
)

type Writer interface {
	Write(t time.Time, v uint16)
	WriteSync(t time.Time, v uint16) error
}

type Store interface {
	Hrt() Writer
	Tmp() Writer
}
