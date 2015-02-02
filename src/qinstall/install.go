package main

import (
	"log"
	"os"
	"os/exec"
	"os/user"
)

func beRoot() {
	u, err := user.Current()
	if err != nil {
		log.Panic(err)
	}

	if u.Uid == "0" {
		return
	}

	cmd := exec.Command("sudo", os.Args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	if err := cmd.Run(); err != nil {
		log.Panic(err)
	}

	os.Exit(0)
}

func main() {
	beRoot()

	u, err := user.Current()
	if err != nil {
		log.Panic(err)
	}
	log.Printf("%s\n", u.Username)
}
