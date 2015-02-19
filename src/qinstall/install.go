package main

import (
	"bytes"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"strings"
)

const (
	systemdCmd = "systemctl"
	upstartCmd = "service"
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

type service interface {
	Start() error
	Stop() error
	UpdateConfig() error
}

type systemd struct {
	name string
}

func (s *systemd) Start() error {
	if err := run("systemctl", "daemon-reload"); err != nil {
		return err
	}

	return run("systemctl", "start", s.name)
}

func (s *systemd) Stop() error {
	return run("systemctl", "stop", s.name)
}

func (s *systemd) UpdateConfig() error {
	return writeAsset(
		fmt.Sprintf("/etc/systemd/system/%s.service", s.name),
		fmt.Sprintf("%s.service", s.name))
}

type upstart struct {
	name string
}

func (s *upstart) Start() error {
	return run("service", s.name, "start")
}

func (s *upstart) Stop() error {
	return run("service", s.name, "stop")
}

func (s *upstart) UpdateConfig() error {
	return writeAsset(
		fmt.Sprintf("/etc/init/%s.conf", s.name),
		fmt.Sprintf("%s.conf", s.name))
}

func run(name string, args ...string) error {
	cmd := exec.Command(name, args...)
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

func pickInitService(sys, svc string) service {
	if sys == "upstart" {
		return &upstart{svc}
	} else if sys == "systemd" {
		return &systemd{svc}
	}

	log.Panic(fmt.Sprintf("invalid init service: %s", sys))
	return nil
}

func main() {
	flagInit := flag.String("init-with", "upstart", "")
	flag.Parse()

	beRoot()

	svc := pickInitService(*flagInit, "qagent")

	if err := svc.Stop(); err != nil {
		log.Print(err)
	}

	if err := svc.UpdateConfig(); err != nil {
		log.Panic(err)
	}

	if err := writeAsset(
		"/usr/local/bin/qagent",
		"qagent"); err != nil {
		log.Panic(err)
	}

	if err := svc.Start(); err != nil {
		log.Panic(err)
	}
}
