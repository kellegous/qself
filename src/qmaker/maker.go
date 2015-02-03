package main

import (
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

func goBuild(dst string, srcs []string, gopath []string, goos, goarch string, goarm int) error {
	args := []string{"build", "-o"}

	args = append(args, dst)

	for _, src := range srcs {
		args = append(args, src)
	}

	env := os.Environ()
	if len(gopath) > 0 {
		paths := make([]string, len(gopath))
		for i, path := range gopath {
			a, err := filepath.Abs(path)
			if err != nil {
				return err
			}
			paths[i] = a
		}
		env = append(env, fmt.Sprintf("GOPATH=%s", strings.Join(paths, ":")))
	}

	if goos != "" {
		env = append(env, fmt.Sprintf("GOOS=%s", goos))
	}

	if goarch != "" {
		env = append(env, fmt.Sprintf("GOARCH=%s", goarch))
	}

	if goarm != 0 {
		env = append(env, fmt.Sprintf("GOARM=%d", goarm))
	}

	cmd := exec.Command("go", args...)
	cmd.Env = env
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	return cmd.Run()
}

func copy(dst, src string) error {
	r, err := os.Open(src)
	if err != nil {
		return err
	}
	defer r.Close()

	w, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer w.Close()

	_, err = io.Copy(w, r)
	return err
}

func scp(src, dst string) error {
	cmd := exec.Command("scp", src, dst)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func runOverSsh(host, sh string) error {
	cmd := exec.Command("ssh", host, sh)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func doBuild(args []string) {
	f := flag.NewFlagSet("build", flag.ContinueOnError)
	f.Parse(args)

	if err := goBuild("bin/qagent",
		[]string{"src/qagent/agent.go"},
		[]string{".", "dep"},
		"", "", 0); err != nil {
		os.Exit(1)
	}

	if err := goBuild("bin/qsensor",
		[]string{"src/qsensor/host.go"},
		[]string{".", "dep"},
		"", "", 0); err != nil {
		os.Exit(1)
	}
}

func getBinData() error {
	if _, err := os.Stat("dep/bin/go-bindata"); err == nil {
		return nil
	}

	p, err := filepath.Abs("dep")
	if err != nil {
		return err
	}

	env := os.Environ()
	env = append(env, fmt.Sprintf("GOPATH=%s", p))

	cmd := exec.Command("go", "get", "github.com/jteeuwen/go-bindata/...")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Env = env
	return cmd.Run()
}

func binData(dst, dir, pfx string) error {
	cmd := exec.Command(
		"dep/bin/go-bindata",
		"--nomemcopy",
		fmt.Sprintf("--prefix=%s", pfx),
		"-o", dst,
		dir)
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	return cmd.Run()
}

func doDeploy(args []string) {
	f := flag.NewFlagSet("deploy", flag.ContinueOnError)
	flagArch := f.String("arch", "arm", "")
	flagOs := f.String("os", "linux", "")
	flagArm := f.Int("arm", 5, "")
	f.Parse(args)

	if f.NArg() != 1 {
		fmt.Fprintf(os.Stderr, "usage %s deploy [options] host", os.Args[0])
		os.Exit(1)
	}

	host := f.Arg(0)

	dst, err := ioutil.TempDir("", "")
	if err != nil {
		log.Panic(err)
	}
	defer os.RemoveAll(dst)

	if err := getBinData(); err != nil {
		os.Exit(1)
	}

	if err := goBuild(filepath.Join(dst, "qagent"),
		[]string{"src/qagent/agent.go"},
		[]string{".", "dep"},
		*flagOs,
		*flagArch,
		*flagArm); err != nil {
		os.Exit(1)
	}

	if err := copy(
		filepath.Join(dst, "qagent.service"),
		"src/qmaker/qagent.service"); err != nil {
		os.Exit(1)
	}

	if err := binData(
		filepath.Join(dst, "data.go"),
		dst,
		dst); err != nil {
		os.Exit(1)
	}

	if err := copy(
		filepath.Join(dst, "install.go"),
		"src/qinstall/install.go"); err != nil {
		log.Println(err)
		os.Exit(1)
	}

	if err := goBuild(filepath.Join(dst, "qinstall"),
		[]string{
			filepath.Join(dst, "install.go"),
			filepath.Join(dst, "data.go"),
		}, []string{".", "dep"},
		*flagOs,
		*flagArch,
		*flagArm); err != nil {
		os.Exit(1)
	}

	if err := scp(
		filepath.Join(dst, "qinstall"),
		fmt.Sprintf("%s:./", host)); err != nil {
		os.Exit(1)
	}

	defer runOverSsh(host, "rm -f qinstall")

	if err := runOverSsh(host, "./qinstall"); err != nil {
		os.Exit(1)
	}
}

func getWd() string {
	return filepath.Join(filepath.Dir(os.Args[0]), "..")
}

func main() {
	if len(os.Args) <= 1 {
		fmt.Fprintf(os.Stderr, "usage: %s command [args]", os.Args[0])
		os.Exit(1)
	}

	if err := os.Chdir(getWd()); err != nil {
		log.Panic(err)
	}

	switch os.Args[1] {
	case "build":
		doBuild(os.Args[2:])
	case "deploy":
		doDeploy(os.Args[2:])
	}
}
