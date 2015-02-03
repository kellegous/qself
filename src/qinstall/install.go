package main

import (
	"bytes"
	"io"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"strings"
)

func getUid() (string, error) {
	cmd := exec.Command("id", "-u")

	r, err := cmd.StdoutPipe()
	if err != nil {
		return "", err
	}
	defer r.Close()

	if err := cmd.Start(); err != nil {
		return "", err
	}

	var buf bytes.Buffer
	if _, err := io.Copy(&buf, r); err != nil {
		return "", err
	}

	if err := cmd.Wait(); err != nil {
		return "", err
	}

	return strings.TrimSpace(buf.String()), nil
}

func beRoot() {
	u, err := getUid()
	if err != nil {
		log.Panic(err)
	}

	if u == "0" {
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

func systemctl(svc, msg string) error {
	cmd := exec.Command("systemctl", msg, svc)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func main() {
	beRoot()

	if err := systemctl("qagent", "stop"); err != nil {
		log.Panic(err)
	}

	b, err := Asset("qagent")
	if err != nil {
		log.Panic(err)
	}

	if err := ioutil.WriteFile(
		"/usr/local/bin/qagent",
		b,
		os.ModePerm); err != nil {
		log.Panic(err)
	}

	if err := systemctl("qagent", "start"); err != nil {
		log.Panic(err)
	}
}
