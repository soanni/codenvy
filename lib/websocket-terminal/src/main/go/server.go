package main

/*
 * websocket/pty proxy server:
 * This program wires a websocket to a pty master.
 *
 * Usage:
 * go build -o ws-pty-proxy server.go
 * ./websocket-terminal -cmd /bin/bash -addr :9000 -static $HOME/src/websocket-terminal
 * ./websocket-terminal -cmd /bin/bash -- -i
 *
 * TODO:
 *  * make more things configurable
 *  * switch back to binary encoding after fixing term.js (see index.html)
 *  * make errors return proper codes to the web client
 *
 * Copyright 2014 Al Tobey tobert@gmail.com
 * MIT License, see the LICENSE file
 */

import (
	"flag"
	"github.com/eclipse/che-lib/websocket"
	"github.com/eclipse/che-lib/pty"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"encoding/json"
	"bufio"
	"bytes"
	"unicode/utf8"
	"regexp"
	"strings"
	"syscall"
	"net"
	"errors"
)

type wsPty struct {
	Cmd     *exec.Cmd // pty builds on os.exec
	PtyFile *os.File  // a pty is simply an os.File
}

type WebSocketMessage struct {
	Type string          `json:"type"`
	Data json.RawMessage `json:"data"`
}

type route struct {
	pattern *regexp.Regexp
	handler http.Handler
}

type routes struct {
	routes []*route
}

var (
	addrFlag, cmdFlag, staticFlag, pathFlag string
	activity = &WorkspaceActivity{}
	upgrader = websocket.Upgrader{
		ReadBufferSize:  1,
		WriteBufferSize: 1,
		CheckOrigin: func(r *http.Request) bool {
			return true
		},
	}
	router = &routes{
		routes : make([]*route, 0, 10),
	}
)

func init() {
	cwd, _ := os.Getwd()
	flag.StringVar(&addrFlag, "addr", ":9000", "IP:PORT or :PORT address to listen on")
	flag.StringVar(&cmdFlag, "cmd", "/bin/bash", "command to execute on slave side of the pty")
	flag.StringVar(&pathFlag, "path", "/", "Path of the pty server. Go regexp syntax is suported.")
	flag.StringVar(&staticFlag, "static", cwd, "path to static content")
	// TODO: make sure paths exist and have correct permissions
}

func main() {
	flag.Parse()

	go activity.StartTracking();

	basePath := pathFlag
	if !strings.HasSuffix(pathFlag, "/") {
		basePath = basePath + "/"
	}
	router.addRouteHandlerFunc(basePath + "pty", ptyHandler)
	// serve html & javascript
	router.addRouteHandler(basePath + ".*", http.FileServer(http.Dir(staticFlag)))

	err := http.ListenAndServe(addrFlag, router)
	if err != nil {
		log.Fatalf("net.http could not listen on address '%s': %s\n", addrFlag, err)
	}
}

func (routes *routes) addRouteHandlerFunc(pattern string, handler func(http.ResponseWriter, *http.Request)) {
	routes.addRouteHandler(pattern, http.HandlerFunc(handler))
}

func (routes *routes) addRouteHandler(pattern string, handler http.Handler) {
	reg, err := regexp.Compile(pattern)
	if err != nil {
		panic("Can't compile pattern:" + pattern)
	}
	router.routes = append(routes.routes, &route{
		pattern : reg,
		handler : handler,
	})
}

func (router *routes) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	for _, route := range router.routes {
		if route.pattern.MatchString(r.URL.Path) {
			route.handler.ServeHTTP(w, r)
			return
		}
	}

	http.NotFound(w, r)
}

func StartPty() (*wsPty, error) {
	// TODO consider whether these args are needed at all
	cmd := exec.Command(cmdFlag, flag.Args()...)
	cmd.Env = append(os.Environ(), "TERM=xterm")

	file, err := pty.Start(cmd)
	if err != nil {
		return nil, err
	}

	//Set the size of the pty
	pty.Setsize(file, 60, 200)

	return &wsPty{
		PtyFile: file,
		Cmd:     cmd,
	}, nil
}

func (wp *wsPty) Stop() {
	wp.PtyFile.Close();
	//kill parent process, but not children processes
	syscall.Kill(wp.Cmd.Process.Pid, syscall.SIGBUS)
	wp.Cmd.Wait()
}

func isNormalWsError(err error) bool {
	closeErr, ok := err.(*websocket.CloseError)
	if ok && (closeErr.Code == websocket.CloseGoingAway || closeErr.Code == websocket.CloseNormalClosure) {
		return true
	}
	_, ok = err.(*net.OpError)
	return ok
}

func isNormalPtyError(err error) bool {
	if err == io.EOF {
		return true
	}
	pathErr, ok := err.(*os.PathError)
	return ok &&
		pathErr.Op == "read" &&
		pathErr.Path == "/dev/ptmx" &&
		pathErr.Err.Error() == "input/output error"
}

// read from the web socket, copying to the pty master
// messages are expected to be text and base64 encoded
func sendConnectionInputToPty(f *os.File, conn *websocket.Conn, done chan bool) {
	defer func() { done <- true }()
	for {
		mt, payload, err := conn.ReadMessage()
		if err != nil {
			if !isNormalWsError(err) {
				log.Printf("conn.ReadMessage failed: %s\n", err)
			}
			return
		}
		switch mt {
		case websocket.BinaryMessage:
			log.Printf("Ignoring binary message: %q\n", payload)
		case websocket.TextMessage:
			var msg WebSocketMessage
			if err := json.Unmarshal(payload, &msg); err != nil {
				log.Printf("Invalid message %s\n", err)
				continue
			}
			if errMsg := handleMessage(msg, f); errMsg != nil {
				log.Printf(errMsg.Error())
				return
			}

		default:
			log.Printf("Invalid websocket message type %d\n", mt)
			return
		}
	}
}

func handleMessage(msg WebSocketMessage, ptyFile *os.File) error {
	switch msg.Type {
	case "resize":
		var size []float64
		if err := json.Unmarshal(msg.Data, &size); err != nil {
			log.Printf("Invalid resize message: %s\n", err)
		} else {
			pty.Setsize(ptyFile, uint16(size[1]), uint16(size[0]))
			activity.Notify();
		}

	case "data":
		var dat string
		if err := json.Unmarshal(msg.Data, &dat); err != nil {
			log.Printf("Invalid data message %s\n", err)
		} else {
			ptyFile.Write([]byte(dat));
			activity.Notify();
		}

	default:
		return errors.New("Invalid field message type: " + msg.Type + "\n")
	}
	return nil
}

//read byte array as Unicode code points (rune in go)
func normalizeBuffer(normalizedBuf *bytes.Buffer, buf []byte, n int) (int, error) {
	bufferBytes := normalizedBuf.Bytes()
	runeReader := bufio.NewReader(bytes.NewReader(append(bufferBytes[:], buf[:n]...)))
	normalizedBuf.Reset()
	i := 0
	for i < n {
		char, charLen, err := runeReader.ReadRune()
		if err != nil {
			return i, err
		}
		if char == utf8.RuneError {
			runeReader.UnreadRune()
			return i, nil
		}
		i += charLen
		if _, err := normalizedBuf.WriteRune(char); err != nil {
			return i, err
		}
	}
	return i, nil
}

// copy everything from the pty master to the websocket
// using base64 encoding for now due to limitations in term.js
func sendPtyOutputToConnection(f *os.File, conn *websocket.Conn, done chan bool) {
	defer func() { done <- true; }()
	buf := make([]byte, 8192)
	reader := bufio.NewReader(f)
	var buffer bytes.Buffer
	// TODO: more graceful exit on socket close / process exit
	for {
		n, err := reader.Read(buf)
		if err != nil {
			if !isNormalPtyError(err) {
				log.Printf("Failed to read from pty: %s", err)
			}
			return
		}
		i, err := normalizeBuffer(&buffer, buf, n)
		if err != nil {
			log.Printf("Cound't normalize byte buffer to UTF-8 sequence, due to an error: %s", err.Error())
			return
		}
		if err = conn.WriteMessage(websocket.TextMessage, buffer.Bytes()); err != nil {
			log.Printf("Failed to send websocket message: %s, due to occurred error %s", string(buffer.Bytes()), err.Error())
			return
		}
		buffer.Reset()
		if i < n {
			buffer.Write(buf[i:n])
		}
	}
}

func ptyHandler(w http.ResponseWriter, r *http.Request) {
	tokenParam := r.URL.Query().Get("token")
	if tokenParam == "" {
		w.WriteHeader(http.StatusUnauthorized)
		log.Println("Authentication failed: missing token.")
		return
	} else {
		req, err := http.NewRequest("GET", apiEndpoint + "/machine/token/user/" + tokenParam, nil)
		if err != nil {
			log.Printf("Authentication failed: %s\n", err)
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		req.Header.Add("Authorization", tokenParam)
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Printf("Authentication failed: %s\n", err)
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		if resp.StatusCode != 200 {
			log.Printf("Authentication failed, token: %s is invalid\n", tokenParam)
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Fatalf("Websocket upgrade failed: %s\n", err)
	}
	defer conn.Close()

	wp, err := StartPty()
	if err != nil {
		log.Fatalf("Failed to start command: %s\n", err)
		return
	}

	done := make(chan bool)

	go sendPtyOutputToConnection(wp.PtyFile, conn, done)
	go sendConnectionInputToPty(wp.PtyFile, conn, done)

	// Block until any routine finishes its work
	<-done

	// Close the pty file and kill the process after
	// any of the routines finished its work, which enforces another
	// go routine to complete and exit
	wp.Stop()
}
