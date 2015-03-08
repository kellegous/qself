package forecast

import (
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"
)

const baseUrl = "https://api.forecast.io/forecast"

type Time struct {
	time.Time
}

func (t *Time) UnmarshalJSON(b []byte) error {
	var e int64
	if err := json.Unmarshal(b, &e); err != nil {
		return err
	}
	t.Time = time.Unix(e, 0)
	return nil
}

func (t *Time) MarshalJSON() ([]byte, error) {
	return t.Time.MarshalJSON()
}

type Area struct {
	Lat    float64
	Lon    float64
	ApiKey string
}

func (a *Area) Get() (*Report, error) {
	res, err := http.Get(fmt.Sprintf("%s/%s/%0.6f,%0.6f", baseUrl, a.ApiKey, a.Lat, a.Lon))
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

type Currently struct {
	Temp                 float64 `json:"temperature"`
	PrecipProbability    float64 `json:"precipProbability"`
	PrecipIntensity      float64 `json:"precipIntensity"`
	NearestStormBearing  float64 `json:"nearestStormBearing"`
	NearestStormDistance float64 `json:"nearestStormDistance"`
	Icon                 string  `json:"icon"`
	Summary              string  `json:"summary"`
	Time                 Time    `json:"time"`
	ApparentTemp         float64 `json:"apparentTemperature"`
	DewPoint             float64 `json:"dewPoint"`
	Humidity             float64 `json:"humidity"`
	WindSpeed            float64 `json:"windSpeed"`
	WindBearing          float64 `json:"windBearing"`
	Visibility           float64 `json:"visibility"`
	CloudCover           float64 `json:"cloudCover"`
	Pressure             float64 `json:"pressure"`
}

type Hourly struct {
	Time              Time    `json:"time"`
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

type Daily struct {
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
	SunsetTime          Time    `json:"sunsetTime"`
	SunriseTime         Time    `json:"sunriseTime"`
	Icon                string  `json:"icon"`
	Summary             string  `json:"summary"`
	Time                Time    `json:"time"`
	PrecipProbability   float64 `json:"precipProbability"`
	TempMin             float64 `json:"temperatureMin"`
	TempMinTime         Time    `json:"temperatureMintime"`
	TempMax             float64 `json:"temperatureMax"`
	TempMaxTime         Time    `json:"temperatureMaxTime"`
	ApparentTempMin     float64 `json:"apperentTemperatureMin"`
	ApparentTempMinTime Time    `json:"apparentTemperatureMinTime"`
	ApparentTempMax     float64 `json:"apparentTemperatureMax"`
}

type Report struct {
	Currently Currently
	Hourly    []Hourly
	Daily     []Daily
}

type service struct {
	Area *Area
	r    *Report
	lck  sync.RWMutex
}

type Service interface {
	Latest() *Report
}

func NewService(a *Area, ttl time.Duration) Service {
	s := &service{
		Area: a,
	}

	go func() {
		for {
			r, err := a.Get()
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

func (r *Report) UnmarshalJSON(b []byte) error {
	var raw struct {
		Currently Currently `json:"currently"`
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
