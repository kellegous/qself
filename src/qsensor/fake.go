package main

import (
	"flag"
	"math/rand"
	"net"
	"time"
)

type noiseSource struct {
	min int64
	max int64
	cur int64
}

func (s *noiseSource) next() int64 {
	if rand.Float64() < 0.5 {
		return s.cur
	}

	v := float64(s.max-s.min) * 0.1
	r := (0.5 - rand.Float64()) * v
	c := s.cur + int64(r)
	if c > s.max {
		c = s.max
	} else if c < s.min {
		c = s.min
	}
	s.cur = c
	return c
}

func FakeStreamToAgent(addr string) error {
	a, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		return err
	}

	c, err := net.DialTCP("tcp", nil, a)
	if err != nil {
		return err
	}
	defer c.Close()

	s := noiseSource{
		min: 800,
		max: 1100,
		cur: 1000,
	}

	var buf [3]byte
	for {
		rr := s.next()

		buf[0] = 0x00
		buf[1] = byte(rr >> 8)
		buf[2] = byte(rr)

		if _, err := c.Write(buf[:]); err != nil {
			return err
		}

		time.Sleep(time.Duration(rr) * time.Millisecond)
	}
}

func main() {
	flagAddr := flag.String("addr", "localhost:8078", "")
	flag.Parse()

	for {
		if err := FakeStreamToAgent(*flagAddr); err != nil {
			time.Sleep(500 * time.Millisecond)
		}
	}
}
