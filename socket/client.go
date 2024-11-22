package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"strings"
	"sync"
)

var (
	connections = make(map[string]net.Conn) // Map of IP to TCP connection
	mutex       sync.Mutex                  // Mutex to handle concurrent access
)

func main() {
	// Get the port from the environment variable or use a default value
	port := os.Getenv("PORT")
	if port == "" {
		port = "5000" // Default port
	}

	address := "0.0.0.0:" + port

	// Start the TCP server
	go startServer(address)

	// Start the command-line interaction
	handleCommands()
}

func startServer(address string) {
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

		clientAddr := conn.RemoteAddr().String()
		fmt.Printf("New connection from %s\n", clientAddr)

		// Add the connection to the hashmap
		mutex.Lock()
		connections[clientAddr] = conn
		mutex.Unlock()

		// Handle incoming messages from this client
		go handleConnection(conn, clientAddr)
	}
}

func handleConnection(conn net.Conn, clientAddr string) {
	defer func() {
		conn.Close()
		mutex.Lock()
		delete(connections, clientAddr)
		mutex.Unlock()
		fmt.Printf("Connection from %s closed\n", clientAddr)
	}()

	reader := bufio.NewReader(conn)
	for {
		message, err := reader.ReadString('\n')
		if err != nil {
			fmt.Printf("Connection closed by %s\n", clientAddr)
			return
		}

		message = strings.TrimSpace(message)
		fmt.Printf("Received from %s: %s\n", clientAddr, message)
	}
}

func handleCommands() {
	reader := bufio.NewReader(os.Stdin)
	for {
		fmt.Print("Enter <ip> <message>: ")
		input, err := reader.ReadString('\n')
		if err != nil {
			fmt.Println("Error reading input:", err)
			continue
		}

		// Process the input
		input = strings.TrimSpace(input)
		parts := strings.SplitN(input, " ", 2)
		if len(parts) != 2 {
			fmt.Println("Invalid input. Format: <ip> <message>")
			continue
		}

		ip, message := parts[0], parts[1]

		// Find the connection and send the message
		mutex.Lock()
		conn, exists := connections[ip]
		mutex.Unlock()
		if !exists {
			fmt.Printf("No active connection to %s\n", ip)
			continue
		}

		_, err = conn.Write([]byte(message + "\n"))
		if err != nil {
			fmt.Printf("Error sending message to %s: %s\n", ip, err)
		} else {
			fmt.Printf("Message sent to %s: %s\n", ip, message)
		}
	}
}
