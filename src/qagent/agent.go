package main

import (
	"encoding/binary"
	"encoding/json"
	"flag"
	"fmt"
	"gopkg.in/yaml.v2"
	"io"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"os"
	"qself/heart"
	"qself/store/pg"
	"strings"
	"sync"
	"time"
)

const (
	heartResetAfter = 2 * time.Second
	gcsPathPrefix   = "qself"
)

const (
	cmdRst byte = 0xff
	cmdHrt byte = 0x00
	cmdTmp byte = 0x01
	cmdUpg byte = 0x02
)

type Context struct {
	lock        sync.RWMutex
	store       *pg.Store
	heartStats  *heart.Stats
	lastHeartAt time.Time
	lastTemp    uint16
	lastTempAt  time.Time
	conns       Conns
}

type Stats struct {
	Hrt struct {
		Active      bool
		Rate        float32
		Variability float32
	}

	Tmp struct {
		Active bool
		Temp   float32
	}
}

type Config struct {
	AgentAddr string `yaml:"AgentAddr"`
	HttpAddr  string `yaml:"HttpAddr"`
	Pg        struct {
		Host     string `yaml:"Host"`
		Database string `yaml:"Database"`
	} `yaml:"Pg"`
}

func (c *Config) Write(filename string) error {
	b, err := yaml.Marshal(c)
	if err != nil {
		return err
	}

	return ioutil.WriteFile(filename, b, os.ModePerm)
}

func (c *Config) PgConnectionString() string {
	cs := []string{
		fmt.Sprintf("dbname=%s", c.Pg.Database),
	}

	if c.Pg.Host != "" {
		cs = append(cs, "sslmode=require")
		cs = append(cs, fmt.Sprintf("host=%s", c.Pg.Host))
	}

	return strings.Join(cs, " ")
}

func tmpFromRaw(raw uint16) float32 {
	return float32(raw) / 100.0
}

func rawFromTmp(tmp float32) uint16 {
	return uint16(tmp * 100.0)
}

func (c *Context) DidReceiveHrm(t time.Time, rr uint16) error {
	if !updateHrm(c, t, rr) {
		return nil
	}

	c.store.Hrt().Write(t, rr)

	return nil
}

func updateHrm(c *Context, t time.Time, rr uint16) bool {
	c.lock.Lock()
	defer c.lock.Unlock()

	hs := c.heartStats

	if t.Sub(c.lastHeartAt) > heartResetAfter {
		hs.Reset()
	}

	c.lastHeartAt = t
	hs.AddInterval(rr)
	return true
}

func updateTmp(c *Context, t time.Time, raw uint16) bool {
	c.lock.Lock()
	defer c.lock.Unlock()

	c.lastTempAt = t
	if raw == c.lastTemp {
		return false
	}

	c.lastTemp = raw
	return true
}

func (c *Context) DidReceiveTmp(t time.Time, raw uint16) error {

	if !updateTmp(c, t, raw) {
		return nil
	}

	c.store.Tmp().Write(t, raw)

	return nil
}

func (c *Context) StatsFor(s *Stats) {
	t := time.Now()

	c.lock.RLock()
	defer c.lock.RUnlock()

	hs := c.heartStats

	if hs.CanReport() && t.Sub(c.lastHeartAt) < 2*time.Second {
		s.Hrt.Active = true
		s.Hrt.Rate = hs.Hr()
		s.Hrt.Variability = float32(hs.HrvLnRmssd20())
	} else {
		s.Hrt.Active = false
		s.Hrt.Rate = 0.0
		s.Hrt.Variability = 0.0
	}

	s.Tmp.Active = t.Sub(c.lastTempAt) < 2*time.Second
	s.Tmp.Temp = tmpFromRaw(c.lastTemp)
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

func ServeBasic(id int, con net.Conn, ctx *Context) {
	defer func() {
		ctx.conns.DidDisconnect(id)
		con.Close()
	}()

	ctx.conns.DidConnect(id)

	var buf [3]byte
	for {
		con.SetDeadline(time.Now().Add(1 * time.Minute))
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
		id := 1
		for {
			con, err := l.Accept()
			if err != nil {
				log.Print(err)
				continue
			}

			go ServeBasic(id, con, ctx)
			id++
		}
	}()

	return nil
}

func MakeContext(ctx *Context, cfg *Config) error {
	s, err := pg.Open(cfg.PgConnectionString())
	if err != nil {
		return err
	}

	ctx.store = s
	ctx.heartStats = heart.NewStats(16, 100)
	return nil
}

func ReadConfig(cfg *Config, filename string) error {
	b, err := ioutil.ReadFile(filename)
	if err != nil {
		return err
	}

	return yaml.Unmarshal(b, &cfg)
}

func writeJson(w http.ResponseWriter, data interface{}) error {
	w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	return json.NewEncoder(w).Encode(data)
}

func RunHttp(addr string, ctx *Context) error {
	http.HandleFunc("/api/status", func(w http.ResponseWriter, r *http.Request) {
		var res Stats
		ctx.StatsFor(&res)

		if err := writeJson(w, &res); err != nil {
			log.Panic(err)
		}
	})

	http.HandleFunc("/api/conns", func(w http.ResponseWriter, r *http.Request) {
		res := ctx.conns.Conns()
		if err := writeJson(w, res); err != nil {
			log.Panic(err)
		}
	})

	return http.ListenAndServe(addr, nil)
}

func main() {
	flagCfg := flag.String("config", "config.yaml", "")
	flag.Parse()

	var cfg Config
	if err := ReadConfig(&cfg, *flagCfg); err != nil {
		log.Panic(err)
	}

	var ctx Context
	if err := MakeContext(&ctx, &cfg); err != nil {
		log.Panic(err)
	}

	if err := ListenForSensors(cfg.AgentAddr, &ctx); err != nil {
		log.Panic(err)
	}

	if err := RunHttp(cfg.HttpAddr, &ctx); err != nil {
		log.Panic(err)
	}
}
