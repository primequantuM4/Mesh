package main

import (
	"bufio"
	"fmt"
	"net"
	"strings"
)

func main() {
	address := "0.0.0.0:5000"

	listener, err := net.Listen("tcp", address)
	if err != nil {
		fmt.Printf("Error starting server: %s\n", err)
		return
	}
	defer listener.Close()

	fmt.Printf("Server started on %s\n", address)

	for {
		conn, err := listener.Accept()
		if err != nil {
			fmt.Printf("Error accepting connection: %s\n", err)
			continue
		}

		go handleConnection(conn)
	}
}

func handleConnection(conn net.Conn) {
	defer conn.Close()

	clientAddr := conn.RemoteAddr().String()
	fmt.Printf("New connection from %s\n", clientAddr)

	reader := bufio.NewReader(conn)
	for {
		message, err := reader.ReadString('\n')
		if err != nil {
			fmt.Printf("Connection closed by %s\n", clientAddr)
			return
		}

		message = strings.TrimSpace(message)
		fmt.Printf("Received from %s: %s\n", clientAddr, message)

		response := fmt.Sprintf("Echo: %s\n", message)
		_, err = conn.Write([]byte(response))
		if err != nil {
			fmt.Printf("Error sending response to %s: %s\n", clientAddr, err)
			return
		}
	}
}
