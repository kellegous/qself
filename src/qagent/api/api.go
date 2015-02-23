package api

import (
	"encoding/json"
	"log"
	"net/http"
	"qagent/ctx"
	"qagent/store"
	"strconv"
	"time"
)

func WriteJson(w http.ResponseWriter, data interface{}) error {
	w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	return json.NewEncoder(w).Encode(data)
}

func Must(err error) {
	if err != nil {
		log.Panic(err)
	}
}

func intParamFrom(r *http.Request, name string, def int) int {
	v := r.FormValue(name)
	i, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return def
	}
	return int(i)
}

type hourlyData struct {
	Time  time.Time
	Avg   float64
	Count int
}

func apiHourly(c *store.Collection, w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/plain")

	startN := intParamFrom(r, "start", 0)
	limitN := intParamFrom(r, "limit", 10)

	start := time.Now().Round(time.Hour).Add(-time.Duration(startN+limitN) * time.Hour)
	limit := start.Add(time.Duration(limitN) * time.Hour)

	res := make([]hourlyData, limitN)
	for i := 0; i < limitN; i++ {
		res[i].Time = start.Add(time.Duration(i) * time.Hour)
	}

	if err := c.ForEachInRange(
		start,
		limit,
		func(t time.Time, v uint16) error {
			ix := int(t.Sub(start).Hours())
			res[ix].Avg += float64(v)
			res[ix].Count++
			return nil
		}); err != nil {
		log.Panic(err)
	}

	for i := 0; i < limitN; i++ {
		if res[i].Count > 0 {
			res[i].Avg /= float64(res[i].Count)
		}
	}

	Must(WriteJson(w, &res))
}

func Setup(m *http.ServeMux, c *ctx.Context) {
	m.HandleFunc("/api/status", func(w http.ResponseWriter, r *http.Request) {
		var res ctx.Stats
		c.StatsFor(&res)
		Must(WriteJson(w, &res))
	})

	m.HandleFunc("/api/hourly/hrt", func(w http.ResponseWriter, r *http.Request) {
		apiHourly(c.Store().Hrt(), w, r)
	})

	m.HandleFunc("/api/hourly/tmp", func(w http.ResponseWriter, r *http.Request) {
		apiHourly(c.Store().Tmp(), w, r)
	})
}
