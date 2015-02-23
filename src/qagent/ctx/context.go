package ctx

import (
	"qagent/heart"
	"qagent/store"
	"qagent/temp"
	"sync"
	"time"
)

const (
	heartResetAfter = 2 * time.Second
)

type Context struct {
	lock        sync.RWMutex
	store       *store.Store
	heartStats  *heart.Stats
	lastHeartAt time.Time
	lastTemp    uint16
	lastTempAt  time.Time
}

type Stats struct {
	Hrt struct {
		Active      bool
		Rate        float64
		Variability float64
	}

	Tmp struct {
		Active bool
		Temp   float64
	}
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
		s.Hrt.Variability = hs.HrvLnRmssd20()
	} else {
		s.Hrt.Active = false
		s.Hrt.Rate = 0.0
		s.Hrt.Variability = 0.0
	}

	s.Tmp.Active = t.Sub(c.lastTempAt) < 2*time.Second
	s.Tmp.Temp = temp.FromRaw(c.lastTemp)
}

func (c *Context) Store() *store.Store {
	return c.store
}

func Make(ctx *Context, dbpath string) error {
	s, err := store.Open(dbpath)
	if err != nil {
		return err
	}

	ctx.store = s
	ctx.heartStats = heart.NewStats(16, 100)
	return nil
}
