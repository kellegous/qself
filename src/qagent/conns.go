package main

import (
	"sync"
	"time"
)

const bufferSize = 5

type ConnInfo struct {
	Id             int
	ConnectedAt    time.Time
	DisconnectedAt time.Time `json:",omitempty"`
}

type JsonData map[string]interface{}

type Conns struct {
	cons []*ConnInfo
	idx  int
	lck  sync.RWMutex
}

func (c *Conns) DidConnect(id int) {
	c.lck.Lock()
	defer c.lck.Unlock()

	if c.cons == nil {
		c.cons = make([]*ConnInfo, 0, bufferSize)
	}

	ci := &ConnInfo{
		Id:          id,
		ConnectedAt: time.Now(),
	}

	n, l := len(c.cons), cap(c.cons)
	if n < l {
		c.cons = append(c.cons, ci)
	} else {
		c.cons[c.idx] = ci
		c.idx = (c.idx + 1) % l
	}
}

func (c *Conns) DidDisconnect(id int) {
	c.lck.Lock()
	defer c.lck.Unlock()

	for i, n := 0, len(c.cons); i < n; i++ {
		ci := c.cons[i]
		if ci.Id == id {
			ci.DisconnectedAt = time.Now()
		}
	}
}

func (c *Conns) Conns() []JsonData {
	c.lck.RLock()
	defer c.lck.RUnlock()

	r := make([]JsonData, 0, len(c.cons))
	for _, ci := range c.cons {
		m := map[string]interface{}{}
		m["id"] = ci.Id
		m["connected_at"] = ci.ConnectedAt
		m["connected"] = ci.DisconnectedAt.IsZero()
		if !ci.DisconnectedAt.IsZero() {
			m["disconnected_at"] = ci.DisconnectedAt
			m["duration_secs"] = ci.DisconnectedAt.Sub(ci.ConnectedAt).Seconds()
		} else {
			m["duration_secs"] = time.Now().Sub(ci.ConnectedAt).Seconds()
		}

		r = append(r, JsonData(m))
	}

	return r
}
