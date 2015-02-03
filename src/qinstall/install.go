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

func systemctl(args ...string) error {
	cmd := exec.Command("systemctl", args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func writeAsset(dst, name string) error {
	b, err := Asset(name)
	if err != nil {
		return err
	}

	return ioutil.WriteFile(dst, b, os.ModePerm)
}

func main() {
	beRoot()

	if err := systemctl("stop", "qagent"); err != nil {
		log.Panic(err)
	}

	if err := writeAsset(
		"/etc/systemd/system/qagent.service",
		"qagent.service"); err != nil {
		log.Panic(err)
	}

	if err := writeAsset(
		"/usr/local/bin/qagent",
		"qagent"); err != nil {
		log.Panic(err)
	}

	if err := systemctl("daemon-reload"); err != nil {
		log.Panic(err)
	}

	if err := systemctl("start", "qagent"); err != nil {
		log.Panic(err)
	}
}
