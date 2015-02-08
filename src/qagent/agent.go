package main

import (
	"encoding/binary"
	"encoding/json"
	"flag"
	"gopkg.in/yaml.v2"
	"io"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"qself/heart"
	"qself/store"
	"qself/upload/gcs"
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
	uploader    *gcs.Client
	lock        sync.RWMutex
	heartStats  *heart.Stats
	heartStore  *store.Writer
	lastHeartAt time.Time
	tempStore   *store.Writer
	lastTemp    uint16
	lastTempAt  time.Time
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
	AgentAddr    string `yaml:"AgentAddr"`
	HttpAddr     string `yaml:"HttpAddr"`
	DataDir      string `yaml:"DataDir"`
	ClientId     string `yaml:"ClientId"`
	ClientSecret string `yaml:"ClientSecret"`
	Bucket       string `yaml:"Bucket"`
	Token        string `yaml:"Token"`
}

func (c *Config) Write(filename string) error {
	b, err := yaml.Marshal(c)
	if err != nil {
		return err
	}

	return ioutil.WriteFile(filename, b, os.ModePerm)
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

	c.heartStore.Write(t, rr)

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

	c.tempStore.Write(t, raw)

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

func ServeBasic(con net.Conn, ctx *Context) {
	defer con.Close()

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

func Upload(up *gcs.Client, service, filename string) error {
	key := filepath.Join(gcsPathPrefix, service, filepath.Base(filename))
	err := up.Upload(key, filename)
	if err == nil {
		return nil
	}

	// TODO(knorton): This should notify me in some way that data is not pipelining
	// back propertly.
	log.Printf("WARNING: upload failed for %s", key)
	return err
}

func MakeContext(ctx *Context, up *gcs.Client, dir string) error {
	hc := store.Config{
		Dir: filepath.Join(dir, "hrm"),
		NeedsUpload: func(filename string) store.UploadOp {
			if err := Upload(up, "hrm", filename); err != nil {
				return store.UploadKeep
			}
			return store.UploadRemove
		},
	}

	tc := store.Config{
		Dir: filepath.Join(dir, "tmp"),
		NeedsUpload: func(filename string) store.UploadOp {
			if err := Upload(up, "tmp", filename); err != nil {
				return store.UploadKeep
			}
			return store.UploadRemove
		},
	}

	hs, err := store.Start(&hc, store.UploadDaily)
	if err != nil {
		return err
	}

	ts, err := store.Start(&tc, store.UploadDaily)
	if err != nil {
		hs.Stop()
		return err
	}

	ctx.heartStats = heart.NewStats(16, 100)
	ctx.heartStore = hs
	ctx.tempStore = ts
	ctx.uploader = up
	return nil
}

func ReadConfig(cfg *Config, filename string) error {
	b, err := ioutil.ReadFile(filename)
	if err != nil {
		return err
	}

	return yaml.Unmarshal(b, &cfg)
}

func Authenticate(filename string, cfg *Config) (*gcs.Client, error) {
	if err := ReadConfig(cfg, filename); err != nil {
		return nil, err
	}

	c := gcs.Config{
		ClientId:     cfg.ClientId,
		ClientSecret: cfg.ClientSecret,
		Bucket:       cfg.Bucket,
		Token:        cfg.Token,
	}

	s, err := gcs.Authenticate(&c)
	if err != nil {
		return nil, err
	}

	cfg.Token = c.Token

	if err := cfg.Write(filename); err != nil {
		return nil, err
	}

	return s, err
}

func RunHttp(addr string, ctx *Context) error {
	http.HandleFunc("/api/status", func(w http.ResponseWriter, r *http.Request) {
		var res Stats
		ctx.StatsFor(&res)

		w.Header().Set("Content-Type", "application/json;charset=UTF-8")
		if err := json.NewEncoder(w).Encode(&res); err != nil {
			log.Panic(err)
		}
	})

	return http.ListenAndServe(addr, nil)
}

func main() {
	flagCfg := flag.String("config", "config.yaml", "")
	flag.Parse()

	var cfg Config
	u, err := Authenticate(*flagCfg, &cfg)
	if err != nil {
		log.Panic(err)
	}

	var ctx Context
	if err := MakeContext(&ctx, u, cfg.DataDir); err != nil {
		log.Panic(err)
	}

	if err := ListenForSensors(cfg.AgentAddr, &ctx); err != nil {
		log.Panic(err)
	}

	if err := RunHttp(cfg.HttpAddr, &ctx); err != nil {
		log.Panic(err)
	}
}
