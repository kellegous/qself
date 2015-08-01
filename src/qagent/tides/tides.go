package tides

import (
	"encoding/json"
	"fmt"
	"log"
	"math"
	"net/http"
	"net/url"
	"strconv"
	"sync"
	"time"

	"qagent/config"
)

const (
	basePredictionsURL = "http://tidesandcurrents.noaa.gov/api/datagetter"
	baseTimesURL       = "http://tidesandcurrents.noaa.gov/noaatidepredictions/StationTideInfo.jsp"
)

// State ...
type State int

const (
	// Invalid is used for debugging only
	Invalid State = iota

	// HighTide ...
	HighTide

	// LowTide ...
	LowTide

	// RisingTide ...
	RisingTide

	// FallingTide ...
	FallingTide
)

func fetch(cfg *config.Config, begin, end string) (*Report, error) {
	r, err := fetchPredictions(cfg.Tides.Station, begin, end)
	if err != nil {
		return nil, err
	}

	if !updatePredictionStates(r.Predictions) {
		return nil, fmt.Errorf("too few predictions: %d", len(r.Predictions))
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

func updatePredictionStates(preds []*Prediction) bool {
	if len(preds) < 3 {
		return false
	}

	// We'll look at a window of 3 values and compute the gradient on each side
	// of the modal value to determine the state of the tide.
	for i, n := 1, len(preds)-1; i < n; i++ {
		a, b, c := preds[i-1], preds[i], preds[i+1]

		// compute the two gradients
		ab := b.Value - a.Value
		bc := c.Value - b.Value

		// we'll now look at the signs of the two gradients to determine if the
		// two time windows represent a saddle point (low and high tides) or a
		// consistency in the direction of change (falling or rising)
		sab, sbc := math.Signbit(ab), math.Signbit(bc)
		if sab != sbc {
			if sab {
				b.State = LowTide
			} else {
				b.State = HighTide
			}
		} else {
			if sab {
				b.State = FallingTide
			} else {
				b.State = RisingTide
			}
		}
	}

	// We're now left with the two end-points lacking state. Neither of these can
	// be a saddle point, so we give them a consistent direction relative to the
	// neighboring data point.
	switch preds[1].State {
	case LowTide, FallingTide:
		preds[0].State = FallingTide
	default:
		preds[0].State = RisingTide
	}

	switch preds[len(preds)-2].State {
	case HighTide, FallingTide:
		preds[len(preds)-1].State = FallingTide
	default:
		preds[len(preds)-1].State = RisingTide
	}

	return true
}

// Prediction ...
type Prediction struct {
	Time  time.Time
	Value float64
	State State
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
