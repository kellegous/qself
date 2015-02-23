package api

import (
	"encoding/json"
	"log"
	"net/http"
	"qagent/ctx"
	"qagent/heart"
	"qagent/store"
	"qagent/temp"
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

func eachHour(c *store.Collection, startN, limitN int, fn func(ix int, t time.Time, v []uint16) error) error {
	start := time.Now().Truncate(time.Hour).Add(-time.Duration(startN+limitN) * time.Hour)
	limit := start.Add(time.Duration(limitN) * time.Hour)

	cix := 0
	var sam []uint16

	if err := c.ForEachInRange(
		start,
		limit,
		func(t time.Time, v uint16) error {
			ix := int(t.Sub(start).Hours())
			if cix == ix {
				sam = append(sam, v)
				return nil
			}

			for cix < ix {
				if err := fn(cix, start.Add(time.Duration(cix)*time.Hour), sam); err != nil {
					return err
				}

				cix++
				sam = sam[:0]
			}

			return nil
		}); err != nil {
		return err
	}

	for cix < limitN {
		if err := fn(cix, start.Add(time.Duration(cix)*time.Hour), sam); err != nil {
			return err
		}

		cix++
		sam = sam[:0]
	}

	return nil
}

type hourlyHrt struct {
	Time  time.Time
	Hr    float64
	Hrv   float64
	Count int
}

func apiHourlyHrt(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	startN := intParamFrom(r, "start", 0)
	limitN := intParamFrom(r, "limit", 10)

	res := make([]hourlyHrt, limitN)

	if err := eachHour(
		c.Store().Hrt(),
		startN,
		limitN,
		func(ix int, t time.Time, vals []uint16) error {
			n := len(vals)
			res[ix].Time = t
			res[ix].Count = n

			if n == 0 {
				return nil
			}

			res[ix].Hr = heart.Hr(vals)
			res[ix].Hrv = heart.HrvLnRmssd20(vals)
			return nil
		}); err != nil {
		log.Panic(err)
	}

	Must(WriteJson(w, &res))
}

type hourlyTmp struct {
	Time  time.Time
	Temp  float64
	Count int
}

func apiHourlyTmp(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	startN := intParamFrom(r, "start", 0)
	limitN := intParamFrom(r, "limit", 10)

	res := make([]hourlyTmp, limitN)

	if err := eachHour(
		c.Store().Tmp(),
		startN,
		limitN,
		func(ix int, t time.Time, vals []uint16) error {
			n := len(vals)
			res[ix].Time = t
			res[ix].Count = n

			if n == 0 {
				return nil
			}

			res[ix].Temp = temp.Average(vals)
			return nil
		}); err != nil {
		log.Panic(err)
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
		apiHourlyHrt(c, w, r)
	})

	m.HandleFunc("/api/hourly/tmp", func(w http.ResponseWriter, r *http.Request) {
		apiHourlyTmp(c, w, r)
	})
}
