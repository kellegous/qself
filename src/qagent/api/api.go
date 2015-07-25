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

func getHourlyHrt(c *ctx.Context, start, limit int) ([]hourlyHrt, error) {
	res := make([]hourlyHrt, limit)
	if err := eachHour(
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

func apiHourlyHrt(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	res, err := getHourlyHrt(c,
		intParamFrom(r, "start", 0),
		intParamFrom(r, "limit", 10))
	if err != nil {
		log.Panic(err)
	}
	Must(WriteJson(w, &res))
}

type hourlyTmp struct {
	Time  time.Time
	Temp  float64
	Count int
}

func getHourlyTmp(c *ctx.Context, start, limit int) ([]hourlyTmp, error) {
	res := make([]hourlyTmp, limit)
	if err := eachHour(
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

func apiHourlyTmp(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	res, err := getHourlyTmp(c,
		intParamFrom(r, "start", 0),
		intParamFrom(r, "limit", 10))
	if err != nil {
		log.Panic(err)
	}
	Must(WriteJson(w, &res))
}

// TODO(knorton): The intent is to do this in parallel.
func apiHourlyAll(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	start := intParamFrom(r, "start", 0)
	limit := intParamFrom(r, "limit", 10)
	tmp, err := getHourlyTmp(c, start, limit)
	if err != nil {
		log.Panic(err)
	}
	hrt, err := getHourlyHrt(c, start, limit)
	if err != nil {
		log.Panic(err)
	}

	res := struct {
		Tmp []hourlyTmp
		Hrt []hourlyHrt
	}{
		tmp,
		hrt,
	}

	Must(WriteJson(w, &res))
}

func latestWeatherOrError(c *ctx.Context, w http.ResponseWriter, r *http.Request) *forecast.Report {
	rep := c.Forecast.Latest()
	if rep == nil {
		http.Error(w, http.StatusText(http.StatusServiceUnavailable),
			http.StatusServiceUnavailable)
	}
	return rep
}

func apiWeatherHourly(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJson(w, rep.Hourly))
	}
}

func apiWeatherCurrent(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJson(w, rep.Currently))
	}
}

func apiWeatherDaily(c *ctx.Context, w http.ResponseWriter, r *http.Request) {
	if rep := latestWeatherOrError(c, w, r); rep != nil {
		Must(WriteJson(w, rep.Daily))
	}
}

// Setup ...
func Setup(m *http.ServeMux, c *ctx.Context) {
	m.HandleFunc("/api/sensors/status", func(w http.ResponseWriter, r *http.Request) {
		var res ctx.Stats
		c.StatsFor(&res)
		Must(WriteJson(w, &res))
	})

	m.HandleFunc("/api/sensors/hourly/hrt", func(w http.ResponseWriter, r *http.Request) {
		apiHourlyHrt(c, w, r)
	})

	m.HandleFunc("/api/sensors/hourly/tmp", func(w http.ResponseWriter, r *http.Request) {
		apiHourlyTmp(c, w, r)
	})

	m.HandleFunc("/api/sensors/hourly/all", func(w http.ResponseWriter, r *http.Request) {
		apiHourlyAll(c, w, r)
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
}
