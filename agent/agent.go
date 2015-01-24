package main

import (
	"encoding/binary"
	"flag"
	"heart"
	"io"
	"log"
	"net"
	"net/http"
	"path/filepath"
	"store"
	"time"
)

const (
	heartResetAfter = 2 * time.Second
)

const (
	cmdRst byte = 0xff
	cmdHrt byte = 0x00
	cmdTmp byte = 0x01
	cmdUpg byte = 0x02
)

type Context struct {
	heartStats  *heart.Stats
	heartStore  *store.Writer
	lastHeartAt time.Time
	tempStore   *store.Writer
	lastTemp    uint16
}

func tmpFromRaw(raw uint16) float32 {
	return float32(raw) / 100.0
}

func rawFromTmp(tmp float32) uint16 {
	return uint16(tmp * 100.0)
}

func (c *Context) DidReceiveHrm(t time.Time, rr uint16) error {
	c.heartStore.Write(t, rr)

	hs := c.heartStats

	if t.Sub(c.lastHeartAt) > heartResetAfter {
		log.Println("resetting heart stats")
		hs.Reset()
	}
	c.lastHeartAt = t

	if hs.AddInterval(rr) {
		log.Printf("hr: %0.2f, hrv (RMSSD): %0.2f, hrv (pNN20): %0.2f",
			hs.Hr(),
			hs.HrvRmssd(),
			hs.HrvPnn20())
	}

	return nil
}

func (c *Context) DidReceiveTmp(t time.Time, raw uint16) error {
	if raw == c.lastTemp {
		return nil
	}

	c.lastTemp = raw
	c.tempStore.Write(t, raw)
	log.Printf("tmp: %0.2f", tmpFromRaw(raw))

	return nil
}

func uintValueFrom(buf []byte) uint16 {
	return uint16(buf[0])<<8 | uint16(buf[1])
}

func ServeAdvanced(con net.Conn, ctx *Context) {
	var cmd byte
	var t int64
	var v uint16

	for {
		if err := binary.Read(con, binary.LittleEndian, &cmd); err != nil {
			log.Println(err)
			return
		}

		if err := binary.Read(con, binary.LittleEndian, &t); err != nil {
			log.Panicln(err)
			return
		}

		if err := binary.Read(con, binary.LittleEndian, &v); err != nil {
			log.Panicln(err)
			return
		}

		switch cmd {
		case cmdHrt:
			if err := ctx.DidReceiveHrm(time.Unix(0, t), v); err != nil {
				log.Panic(err)
			}
		case cmdTmp:
			if err := ctx.DidReceiveTmp(time.Unix(0, t), v); err != nil {
				log.Panic(err)
			}
		case cmdRst, cmdUpg:
			continue
		}
	}
}

func ServeBasic(con net.Conn, ctx *Context) {
	defer con.Close()

	var buf [3]byte
	for {
		if _, err := io.ReadFull(con, buf[:]); err != nil {
			log.Println(err)
			return
		}

		switch buf[0] {
		case cmdUpg:
			ServeAdvanced(con, ctx)
			return
		case cmdRst:
			continue
		case cmdHrt:
			if err := ctx.DidReceiveHrm(time.Now(), uintValueFrom(buf[1:])); err != nil {
				log.Print(err)
			}
		case cmdTmp:
			if err := ctx.DidReceiveTmp(time.Now(), uintValueFrom(buf[1:])); err != nil {
				log.Print(err)
			}
		default:
			log.Print("invalid command: %d", buf[0])
			return
		}
	}
}

func ListenForSensors(addr string, ctx *Context) error {
	a, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		return err
	}

	l, err := net.ListenTCP("tcp", a)
	if err != nil {
		return err
	}

	go func() {
		for {
			con, err := l.Accept()
			if err != nil {
				log.Print(err)
				continue
			}

			go ServeBasic(con, ctx)
		}
	}()

	return nil
}

func MakeContext(ctx *Context, dir string) error {
	hc := store.Config{
		Dir: filepath.Join(dir, "hrm"),
		NeedsUpload: func(filename string) store.UploadOp {
			return store.UploadRemove
		},
	}

	tc := store.Config{
		Dir: filepath.Join(dir, "tmp"),
		NeedsUpload: func(filename string) store.UploadOp {
			return store.UploadRemove
		},
	}

	hs, err := store.Start(&hc)
	if err != nil {
		return err
	}

	ts, err := store.Start(&tc)
	if err != nil {
		// TODO(knorton): shutdown hs
		return err
	}

	ctx.heartStats = heart.NewStats(16, 100)
	ctx.heartStore = hs
	ctx.tempStore = ts
	return nil
}

func main() {
	flagHttpAddr := flag.String("http", ":8077", "")
	flagAgntAddr := flag.String("agnt", ":8078", "")

	flag.Parse()

	var ctx Context
	if err := MakeContext(&ctx, "data"); err != nil {
		log.Panic(err)
	}

	if err := ListenForSensors(*flagAgntAddr, &ctx); err != nil {
		log.Panic(err)
	}

	if err := http.ListenAndServe(*flagHttpAddr, nil); err != nil {
		log.Panic(err)
	}
}
