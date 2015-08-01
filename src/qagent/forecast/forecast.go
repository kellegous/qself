package forecast

import (
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"

	"qagent/config"
)

const baseURL = "https://api.forecast.io/forecast"

func fetch(cfg *config.Config) (*Report, error) {
	res, err := http.Get(fmt.Sprintf("%s/%s/%0.6f,%0.6f",
		baseURL,
		cfg.Forecast.APIKey,
		cfg.Forecast.Lat,
		cfg.Forecast.Lon))
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

// Currently ...
type Currently struct {
	Temp                 float64
	PrecipProbability    float64
	PrecipIntensity      float64
	NearestStormBearing  float64
	NearestStormDistance float64
	Icon                 string
	Summary              string
	Time                 time.Time
	ApparentTemp         float64
	DewPoint             float64
	Humidity             float64
	WindSpeed            float64
	WindBearing          float64
	Visibility           float64
	CloudCover           float64
	Pressure             float64
}

// UnmarshalJSON ...
func (c *Currently) UnmarshalJSON(b []byte) error {
	var s struct {
		Temp                 float64 `json:"temperature"`
		PrecipProbability    float64 `json:"precipProbability"`
		PrecipIntensity      float64 `json:"precipIntensity"`
		NearestStormBearing  float64 `json:"nearestStormBearing"`
		NearestStormDistance float64 `json:"nearestStormDistance"`
		Icon                 string  `json:"icon"`
		Summary              string  `json:"summary"`
		Time                 int64   `json:"time"`
		ApparentTemp         float64 `json:"apparentTemperature"`
		DewPoint             float64 `json:"dewPoint"`
		Humidity             float64 `json:"humidity"`
		WindSpeed            float64 `json:"windSpeed"`
		WindBearing          float64 `json:"windBearing"`
		Visibility           float64 `json:"visibility"`
		CloudCover           float64 `json:"cloudCover"`
		Pressure             float64 `json:"pressure"`
	}

	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}

	c.Temp = s.Temp
	c.PrecipProbability = s.PrecipProbability
	c.PrecipIntensity = s.PrecipIntensity
	c.NearestStormBearing = s.NearestStormBearing
	c.NearestStormDistance = s.NearestStormDistance
	c.Icon = s.Icon
	c.Summary = s.Summary
	c.Time = time.Unix(s.Time, 0)
	c.ApparentTemp = s.ApparentTemp
	c.DewPoint = s.DewPoint
	c.Humidity = s.Humidity
	c.WindSpeed = s.WindSpeed
	c.WindBearing = s.WindBearing
	c.Visibility = s.Visibility
	c.CloudCover = s.CloudCover
	c.Pressure = s.Pressure
	return nil
}

// Hourly ...
type Hourly struct {
	Time              time.Time
	Summary           string
	Icon              string
	Temp              float64
	ApparentTemp      float64
	WindSpeed         float64
	WindBearing       float64
	Pressure          float64
	Humidity          float64
	PrecipIntensity   float64
	PrecipProbability float64
	DewPoint          float64
	Visibility        float64
	CloudCover        float64
}

// UnmarshalJSON ...
func (h *Hourly) UnmarshalJSON(b []byte) error {
	var s struct {
		Time              int64   `json:"time"`
		Summary           string  `json:"summary"`
		Icon              string  `json:"icon"`
		Temp              float64 `json:"temperature"`
		ApparentTemp      float64 `json:"apparentTemperature"`
		WindSpeed         float64 `json:"windSpeed"`
		WindBearing       float64 `json:"windBearing"`
		Pressure          float64 `json:"pressure"`
		Humidity          float64 `json:"humidity"`
		PrecipIntensity   float64 `json:"precipIntensity"`
		PrecipProbability float64 `json:"precipProbability"`
		DewPoint          float64 `json:"dewPoint"`
		Visibility        float64 `json:"visibility"`
		CloudCover        float64 `json:"cloudCover"`
	}

	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}

	h.Time = time.Unix(s.Time, 0).UTC()
	h.Summary = s.Summary
	h.Icon = s.Icon
	h.Temp = s.Temp
	h.ApparentTemp = s.ApparentTemp
	h.WindSpeed = s.WindSpeed
	h.WindBearing = s.WindBearing
	h.Pressure = s.Pressure
	h.Humidity = s.Humidity
	h.PrecipIntensity = s.PrecipIntensity
	h.PrecipProbability = s.PrecipProbability
	h.DewPoint = s.DewPoint
	h.Visibility = s.Visibility
	h.CloudCover = s.CloudCover
	return nil
}

// Daily ...
type Daily struct {
	Pressure            float64
	CloudCover          float64
	Visibility          float64
	WindBearing         float64
	WindSpeed           float64
	Humidity            float64
	DewPoint            float64
	ApparentTemp        float64
	PrecipIntensityMax  float64
	PrecipIntensity     float64
	MoonPhase           float64
	SunsetTime          time.Time
	SunriseTime         time.Time
	Icon                string
	Summary             string
	Time                time.Time
	PrecipProbability   float64
	TempMin             float64
	TempMinTime         time.Time
	TempMax             float64
	TempMaxTime         time.Time
	ApparentTempMin     float64
	ApparentTempMinTime time.Time
	ApparentTempMax     float64
}

// UnmarshalJSON ...
func (d *Daily) UnmarshalJSON(b []byte) error {
	var s struct {
		Pressure            float64 `json:"pressure"`
		CloudCover          float64 `json:"cloudCover"`
		Visibility          float64 `json:"visibility"`
		WindBearing         float64 `json:"windBearing"`
		WindSpeed           float64 `json:"windSpeed"`
		Humidity            float64 `json:"humidity"`
		DewPoint            float64 `json:"dewPoint"`
		ApparentTemp        float64 `json:"apparentTemperature"`
		PrecipIntensityMax  float64 `json:"precipIntensityMax"`
		PrecipIntensity     float64 `json:"precipIntensity"`
		MoonPhase           float64 `json:"moonPhase"`
		SunsetTime          int64   `json:"sunsetTime"`
		SunriseTime         int64   `json:"sunriseTime"`
		Icon                string  `json:"icon"`
		Summary             string  `json:"summary"`
		Time                int64   `json:"time"`
		PrecipProbability   float64 `json:"precipProbability"`
		TempMin             float64 `json:"temperatureMin"`
		TempMinTime         int64   `json:"temperatureMintime"`
		TempMax             float64 `json:"temperatureMax"`
		TempMaxTime         int64   `json:"temperatureMaxTime"`
		ApparentTempMin     float64 `json:"apperentTemperatureMin"`
		ApparentTempMinTime int64   `json:"apparentTemperatureMinTime"`
		ApparentTempMax     float64 `json:"apparentTemperatureMax"`
	}

	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}

	d.Pressure = s.Pressure
	d.CloudCover = s.CloudCover
	d.Visibility = s.Visibility
	d.WindBearing = s.WindBearing
	d.WindSpeed = s.WindSpeed
	d.Humidity = s.Humidity
	d.DewPoint = s.DewPoint
	d.ApparentTemp = s.ApparentTemp
	d.PrecipIntensityMax = s.PrecipIntensityMax
	d.PrecipIntensity = s.PrecipIntensity
	d.MoonPhase = s.MoonPhase
	d.SunsetTime = time.Unix(s.SunsetTime, 0)
	d.SunriseTime = time.Unix(s.SunriseTime, 0)
	d.Icon = s.Icon
	d.Summary = s.Summary
	d.Time = time.Unix(s.Time, 0)
	d.PrecipProbability = s.PrecipProbability
	d.TempMin = s.TempMin
	d.TempMinTime = time.Unix(s.TempMinTime, 0)
	d.TempMax = s.TempMax
	d.TempMaxTime = time.Unix(s.TempMaxTime, 0)
	d.ApparentTempMin = s.ApparentTempMin
	d.ApparentTempMinTime = time.Unix(s.ApparentTempMinTime, 0)
	d.ApparentTempMax = s.ApparentTempMax
	return nil
}

// Report ...
type Report struct {
	Currently *Currently
	Hourly    []Hourly
	Daily     []Daily
}

type service struct {
	r   *Report
	lck sync.RWMutex
}

// Service ...
type Service interface {
	Latest() *Report
}

// NewService ...
func NewService(cfg *config.Config, ttl time.Duration) Service {
	s := &service{}

	go func() {
		for {
			r, err := fetch(cfg)
			if err == nil {
				s.lck.Lock()
				s.r = r
				s.lck.Unlock()
			}

			time.Sleep(ttl)
		}
	}()

	return s
}

func (s *service) Latest() *Report {
	s.lck.RLock()
	defer s.lck.RUnlock()
	return s.r
}

// UnmarshalJSON ...
func (r *Report) UnmarshalJSON(b []byte) error {
	var raw struct {
		Currently *Currently `json:"currently"`
		Hourly    struct {
			Data []Hourly `json:"data"`
		}
		Daily struct {
			Data []Daily `json:"data"`
		}
	}

	if err := json.Unmarshal(b, &raw); err != nil {
		return err
	}

	r.Currently = raw.Currently
	r.Hourly = raw.Hourly.Data
	r.Daily = raw.Daily.Data
	return nil
}
