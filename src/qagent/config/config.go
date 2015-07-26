package config

import (
	"io/ioutil"
	"os"

	"gopkg.in/yaml.v2"
)

// Config ...
type Config struct {
	AgentAddr string `yaml:"AgentAddr"`
	HTTPAddr  string `yaml:"HttpAddr"`
	Db        string `yaml:"DB"`
	Forecast  struct {
		APIKey string  `yaml:"ApiKey"`
		Lat    float64 `yaml:"Lat"`
		Lon    float64 `yaml:"Lon"`
	} `yaml:"Forecast"`
	Tides struct {
		Station string `yaml:"Station"`
	} `yaml:"Tides"`
}

// ReadFromFile ...
func (c *Config) ReadFromFile(filename string) error {
	b, err := ioutil.ReadFile(filename)
	if err != nil {
		return err
	}

	return yaml.Unmarshal(b, c)
}

// WriteFile ...
func (c *Config) WriteFile(filename string) error {
	b, err := yaml.Marshal(c)
	if err != nil {
		return err
	}

	return ioutil.WriteFile(filename, b, os.ModePerm)
}
