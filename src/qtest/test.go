package main

import (
	"log"
	"qself/store/pg"
	"time"
)

func main() {
	s, err := pg.Open("host=flint dbname=knorton sslmode=require")
	if err != nil {
		log.Panic(err)
	}

	for i := 0; i < 100; i++ {
		s.Tmp.Write(time.Now(), uint16(i))
	}
}
