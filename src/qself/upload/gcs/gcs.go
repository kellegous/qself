package gcs

import (
	"bytes"
	"encoding/base64"
	"fmt"
	"github.com/kellegous/coauth"
	"google.golang.org/api/storage/v1"
	"io"
	"log"
	"mime"
	"net/http"
	"os"
	"path/filepath"
)

type Config struct {
	ClientId     string
	ClientSecret string
	Bucket       string
	Token        string
}

type Client struct {
	s *storage.Service
	b string
}

type readerWithType struct {
	io.Reader
	Type string
}

func (r *readerWithType) ContentType() string {
	return r.Type
}

func (c *Client) Upload(key, filename string) error {
	log.Printf("%s => %s", filename, key)
	r, err := os.Open(filename)
	if err != nil {
		return err
	}
	defer r.Close()

	_, err = c.s.Objects.Insert(c.b, &storage.Object{
		Name: key,
		Acl: []*storage.ObjectAccessControl{
			&storage.ObjectAccessControl{
				Entity: "allUsers",
				Role:   "READER",
			},
		},
	}).Media(&readerWithType{
		Reader: r,
		Type:   mime.TypeByExtension(filepath.Ext(filename)),
	}).Do()
	return err
}

func openTransport(cfg *Config) (*coauth.Client, error) {
	cc := coauth.Config{
		ClientId:     cfg.ClientId,
		ClientSecret: cfg.ClientSecret,
		Scope:        storage.DevstorageFull_controlScope,
		AuthUrl:      "https://accounts.google.com/o/oauth2/auth",
		TokenUrl:     "https://accounts.google.com/o/oauth2/token",
	}

	if cfg.Token != "" {
		b, err := base64.StdEncoding.DecodeString(cfg.Token)
		if err != nil {
			return nil, err
		}

		return coauth.ReadClient(bytes.NewReader(b), &cc)
	}

	c, err := coauth.Authenticate(&cc, func(url string) error {
		fmt.Printf("Please visit this url: %s\n", url)
		return nil
	})
	if err != nil {
		return nil, err
	}

	var buf bytes.Buffer
	if err := c.Write(&buf); err != nil {
		return nil, err
	}

	cfg.Token = base64.StdEncoding.EncodeToString(buf.Bytes())

	return c, nil
}

func Authenticate(cfg *Config) (*Client, error) {
	t, err := openTransport(cfg)
	if err != nil {
		return nil, err
	}

	s, err := storage.New(&http.Client{
		Transport: t,
	})
	if err != nil {
		return nil, err
	}

	return &Client{s: s, b: cfg.Bucket}, nil
}
