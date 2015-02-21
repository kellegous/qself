package api

import (
	"encoding/json"
	"log"
	"net/http"
	"qagent/ctx"
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

func Setup(m *http.ServeMux, c *ctx.Context) {
	m.HandleFunc("/api/status", func(w http.ResponseWriter, r *http.Request) {
		var res ctx.Stats
		c.StatsFor(&res)
		Must(WriteJson(w, &res))
	})
}
