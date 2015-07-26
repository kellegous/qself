package tides

import (
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"qagent/config"
)

const (
	basePredictionsURL = "http://tidesandcurrents.noaa.gov/api/datagetter"
	baseTimesURL       = "http://tidesandcurrents.noaa.gov/noaatidepredictions/StationTideInfo.jsp"
)

func fetch(cfg *config.Config, begin, end string) (*Report, error) {
	r, err := fetchPredictions(cfg.Tides.Station, begin, end)
	if err != nil {
		return nil, err
	}

	now := time.Now()
	tides, err := fetchTimes(now, cfg.Tides.Station)
	if err != nil {
		return nil, err
	}

	for _, tide := range tides {
		if tide.High {
			r.HighTides = append(r.HighTides, tide.Time)
		} else {
			r.LowTides = append(r.LowTides, tide.Time)
		}
	}

	return r, nil
}

func fetchPredictions(station, begin, end string) (*Report, error) {
	res, err := http.Get(fmt.Sprintf("%s?%s", basePredictionsURL, url.Values{
		"datum":       {"MLLW"},
		"time_zone":   {"gmt"},
		"units":       {"english"},
		"product":     {"predictions"},
		"application": {"NOS.COOPS.TAC.WL"},
		"format":      {"json"},
		"begin_date":  {begin},
		"end_date":    {end},
		"station":     {station},
	}.Encode()))
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	var r Report
	if err := json.NewDecoder(res.Body).Decode(&r); err != nil {
		return nil, err
	}

	return &r, nil
}

func findTimeRelativeToCurrent(cur time.Time, t time.Time) time.Time {
	padded := cur.Add(-15 * time.Minute)
	today := time.Date(
		cur.Year(),
		cur.Month(),
		cur.Day(),
		t.Hour(),
		t.Minute(),
		0,
		0,
		cur.Location())
	if today.After(padded) {
		return today
	}

	cur = cur.Add(24 * time.Hour)
	return time.Date(
		cur.Year(),
		cur.Month(),
		cur.Day(),
		t.Hour(),
		t.Minute(),
		0,
		0,
		cur.Location())
}

func fetchTimes(cur time.Time, station string) ([]*Time, error) {
	res, err := http.Get(fmt.Sprintf("%s?%s", baseTimesURL, url.Values{
		"Stationid": {station},
		"timeZone":  {"2"},
	}.Encode()))
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	var times []*Time
	s := bufio.NewScanner(res.Body)
	for s.Scan() {
		e := s.Text()

		ix := strings.Index(e, "|")
		if ix == -1 {
			continue
		}

		t, err := time.Parse("3:04 PM", e[:ix])
		if err != nil {
			continue
		}

		ix = strings.LastIndex(e, "|")
		if ix == -1 {
			continue
		}

		cur = findTimeRelativeToCurrent(cur, t)
		tt := &Time{
			Time: cur.UTC(),
		}

		switch e[ix+1:] {
		case "high":
			tt.High = true
		case "low":
			tt.High = false
		default:
			continue
		}

		times = append(times, tt)
	}

	if err := s.Err(); err != nil {
		return nil, err
	}

	return times, nil
}

// Prediction ...
type Prediction struct {
	Time  time.Time
	Value float64
}

// Time ...
type Time struct {
	Time time.Time
	High bool
}

// UnmarshalJSON ...
func (p *Prediction) UnmarshalJSON(b []byte) error {
	var s struct {
		T string `json:"t"`
		V string `json:"v"`
	}

	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}

	t, err := time.Parse("2006-01-02 15:04", s.T)
	if err != nil {
		return err
	}

	v, err := strconv.ParseFloat(s.V, 64)
	if err != nil {
		return err
	}

	p.Time = t
	p.Value = v
	return nil
}

// Report ...
type Report struct {
	Predictions []*Prediction
	LowTides    []time.Time
	HighTides   []time.Time
}

type service struct {
	r *Report
	m sync.RWMutex
}

func (s *service) Latest() *Report {
	s.m.RLock()
	defer s.m.RUnlock()
	return s.r
}

// Service ...
type Service interface {
	Latest() *Report
}

func currentBeginAndEndDays() (string, string) {
	b := time.Now().UTC()
	e := b.Add(24 * time.Hour)
	return b.Format("20060102"), e.Format("20060102")
}

// NewService ...
func NewService(cfg *config.Config, ttl time.Duration) Service {
	s := &service{}

	go func() {
		for {
			b, e := currentBeginAndEndDays()
			r, err := fetch(cfg, b, e)
			if err == nil {
				s.m.Lock()
				s.r = r
				s.m.Unlock()
			} else {
				log.Printf("ERROR: %s", err)
			}

			time.Sleep(ttl)
		}
	}()

	return s
}
