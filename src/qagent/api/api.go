package api

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"time"

	"qagent/ctx"
	"qagent/forecast"
	"qagent/heart"
	"qagent/store"
	"qagent/temp"
	"qagent/tides"
)

var hourly = &grouper{
	d: time.Hour,
	f: func(s time.Time, t time.Time) int {
		return int(t.Sub(s).Hours())
	},
}

var minutely = &grouper{
	d: time.Minute,
	f: func(s time.Time, t time.Time) int {
		return int(t.Sub(s).Seconds() / 60)
	},
}

// WriteJSON ...
func WriteJSON(w http.ResponseWriter, data interface{}) error {
	w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	return json.NewEncoder(w).Encode(data)
}

// Must ...
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

type grouper struct {
	d time.Duration
	f func(s time.Time, t time.Time) int
}

func (g *grouper) IndexFor(s time.Time, t time.Time) int {
	return g.f(s, t)
}

func groupBy(
	g *grouper,
	c *store.Collection,
	startN, limitN int,
	fn func(ix int, t time.Time, v []uint16) error) error {
	start := time.Now().Truncate(g.d).Add(-time.Duration(startN+limitN) * g.d)
	limit := start.Add(time.Duration(limitN) * g.d)

	cix := 0
	var sam []uint16

	if err := c.ForEachInRange(
		start,
		limit,
		func(t time.Time, v uint16) error {
			ix := g.IndexFor(start, t)
			if cix == ix {
				sam = append(sam, v)
				return nil
			}

			for cix < ix {
				if err := fn(cix, start.Add(time.Duration(cix)*g.d), sam); err != nil {
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
		if err := fn(cix, start.Add(time.Duration(cix)*g.d), sam); err != nil {
			return err
		}

		cix++
		sam = sam[:0]
	}

	return nil
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

type hrtGroup struct {
	Time  time.Time
	Hr    float64
	Hrv   float64
	Count int
}

type tmpGroup struct {
	Time  time.Time
	Temp  float64
	Count int
}

func getHrtGroups(
	c *ctx.Context,
	g *grouper,
	start, limit int) ([]hrtGroup, error) {
	res := make([]hrtGroup, limit)
	if err := groupBy(
		g,
		c.Store().Hrt(),
		start,
		limit,
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
		return nil, err
	}

	return res, nil
}

func getTmpGroups(
	c *ctx.Context,
	g *grouper,
	start, limit int) ([]tmpGroup, error) {
	res := make([]tmpGroup, limit)
	if err := groupBy(
		g,
		c.Store().Tmp(),
		start,
		limit,
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
		return nil, err
	}
	return res, nil
}

func apiGroupHrt(c *ctx.Context, g *grouper, w http.ResponseWriter, r *http.Request) {
	res, err := getHrtGroups(c, g,
		intParamFrom(r, "start", 0),
		intParamFrom(r, "limit", 10))
	if err != nil {
		log.Panic(err)
	}
	Must(WriteJSON(w, &res))
}

func apiGroupTmp(c *ctx.Context, g *grouper, w http.ResponseWriter, r *http.Request) {
	res, err := getTmpGroups(c, g,
		intParamFrom(r, "start", 0),
		intParamFrom(r, "limit", 10))
	if err != nil {
		log.Panic(err)
	}
	Must(WriteJSON(w, &res))
}

// TODO(knorton): The intent is to do this in parallel.
func apiGroupAll(c *ctx.Context, g *grouper, w http.ResponseWriter, r *http.Request) {
	start := intParamFrom(r, "start", 0)
	limit := intParamFrom(r, "limit", 10)
	tmp, err := getTmpGroups(c, g, start, limit)
	if err != nil {
		log.Panic(err)
	}
	hrt, err := getHrtGroups(c, g, start, limit)
	if err != nil {
		log.Panic(err)
	}

	res := struct {
		Tmp []tmpGroup
		Hrt []hrtGroup
	}{
		tmp,
		hrt,
	}

	Must(WriteJSON(w, &res))
}

func latestWeatherOrError(c *ctx.Context, w http.ResponseWriter, r *http.Request) *forecast.Report {
	rep := c.Forecast.Latest()
	if rep == nil {
		http.Error(w, http.StatusText(http.StatusServiceUnavailable),
			http.StatusServiceUnavailable)
	}
	return rep
}

func timeFromPrediction(p *tides.Prediction) time.Time {
	if p == nil {
		return time.Time{}
	}
	return p.Time
}

func indexOfFirstAfter(prds []*tides.Prediction, t time.Time) int {
	for i, n := 0, len(prds); i < n; i++ {
		if prds[i].Time.After(t) {
			return i
		}
	}
	return -1
}

func indexOfFirstWithState(prds []*tides.Prediction, s tides.State) int {
	for i, n := 0, len(prds); i < n; i++ {
		if prds[i].State == s {
			return i
		}
	}
	return -1
}

func apiTides(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	rep := c.Tides.Latest()
	if rep == nil {
		http.Error(w, http.StatusText(http.StatusServiceUnavailable),
			http.StatusServiceUnavailable)
		return
	}

	now := time.Now()
	prds := rep.FromRange(now.Add(-1*time.Hour), now.Add(23*time.Hour))

	res := struct {
		Predictions  []*tides.Prediction
		NextHighTide int
		NextLowTide  int
		Now          int
	}{
		prds,
		indexOfFirstWithState(prds, tides.HighTide),
		indexOfFirstWithState(prds, tides.LowTide),
		indexOfFirstAfter(prds, now),
	}

	Must(WriteJSON(w, &res))
}

func apiWeatherHourly(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJSON(w, rep.Hourly))
	}
}

func apiWeatherCurrent(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJSON(w, rep.Currently))
	}
}

func apiWeatherDaily(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJSON(w, rep.Daily))
	}
}

// Setup ...
func Setup(m *http.ServeMux, c *ctx.Context) {
	m.HandleFunc("/api/sensors/status", func(w http.ResponseWriter, r *http.Request) {
		var res ctx.Stats
		c.StatsFor(&res)
		Must(WriteJSON(w, &res))
	})

	m.HandleFunc("/api/sensors/hourly/hrt", func(w http.ResponseWriter, r *http.Request) {
		apiGroupHrt(c, hourly, w, r)
	})

	m.HandleFunc("/api/sensors/minutely/hrt", func(w http.ResponseWriter, r *http.Request) {
		apiGroupHrt(c, minutely, w, r)
	})

	m.HandleFunc("/api/sensors/hourly/tmp", func(w http.ResponseWriter, r *http.Request) {
		apiGroupTmp(c, hourly, w, r)
	})

	m.HandleFunc("/api/sensors/minutely/tmp", func(w http.ResponseWriter, r *http.Request) {
		apiGroupTmp(c, minutely, w, r)
	})

	m.HandleFunc("/api/sensors/hourly/all", func(w http.ResponseWriter, r *http.Request) {
		apiGroupAll(c, hourly, w, r)
	})

	m.HandleFunc("/api/sensors/minutely/all", func(w http.ResponseWriter, r *http.Request) {
		apiGroupAll(c, minutely, w, r)
	})

	m.HandleFunc("/api/weather/current", func(w http.ResponseWriter, r *http.Request) {
		apiWeatherCurrent(c, w, r)
	})

	m.HandleFunc("/api/weather/hourly", func(w http.ResponseWriter, r *http.Request) {
		apiWeatherHourly(c, w, r)
	})

	m.HandleFunc("/api/weather/daily", func(w http.ResponseWriter, r *http.Request) {
		apiWeatherDaily(c, w, r)
	})

	m.HandleFunc("/api/tides/predictions", func(w http.ResponseWriter, r *http.Request) {
		apiTides(c, w, r)
	})
}
