package ctx

import (
	"sync"
	"time"

	"qagent/config"
	"qagent/forecast"
	"qagent/heart"
	"qagent/store"
	"qagent/temp"
	"qagent/tides"
)

const (
	heartResetAfter = 2 * time.Second
)

// Context ...
type Context struct {
	lock        sync.RWMutex
	store       *store.Store
	heartStats  *heart.Stats
	lastHeartAt time.Time
	lastTemp    uint16
	lastTempAt  time.Time
	Forecast    forecast.Service
	Tides       tides.Service
}

// Stats ...
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

// DidReceiveHrm ...
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

// DidReceiveTmp ...
func (c *Context) DidReceiveTmp(t time.Time, raw uint16) error {

	if !updateTmp(c, t, raw) {
		return nil
	}

	c.store.Tmp().Write(t, raw)

	return nil
}

// StatsFor ...
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

// Store ...
func (c *Context) Store() *store.Store {
	return c.store
}

// Make ...
func Make(ctx *Context, cfg *config.Config) error {
	s, err := store.Open(cfg.Db)
	if err != nil {
		return err
	}

	ctx.store = s
	ctx.heartStats = heart.NewStats(16, 100)
	ctx.Forecast = forecast.NewService(cfg, 15*time.Minute)
	ctx.Tides = tides.NewService(cfg, 15*time.Minute)
	return nil
}
