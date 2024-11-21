import socket
import threading

class BluetoothServer:
    def __init__(self, host, port):
        self.server_sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
        self.server_sock.bind((host, port))
        self.server_sock.listen(5)
        self.clients = []

    def handle_client(self, client_sock, client_info):
        print(f"Accepted connection from {client_info}")
        self.broadcast_message(f"Connected with {client_info}")

        try:
            while True:
                data = client_sock.recv(1024)
                if not data:
                    break
                print(f"Received data from {client_info}: {data.decode('utf-8')}")
                self.broadcast_message(f"Received message from {client_info}: {data.decode('utf-8')}")
        except OSError:
            pass

        print(f"Disconnected from {client_info}")
        self.clients.remove(client_sock)
        client_sock.close()

    def broadcast_message(self, message):
        for client in self.clients:
            try:
                client.send(message.encode())
            except OSError:
                pass

    def run(self):
        print("Waiting for connections...")
        try:
            while True:
                client_sock, client_info = self.server_sock.accept()
                self.clients.append(client_sock)
                client_thread = threading.Thread(target=self.handle_client, args=(client_sock, client_info))
                client_thread.start()
        except KeyboardInterrupt:
            print("Server shutting down")
        finally:
            self.server_sock.close()

if __name__ == "__main__":
    host = "10:5b:ad:8b:bc:3c"  # this is my pc's Replace with your Bluetooth adapter address
    port = 4
    server = BluetoothServer(host, port)
    server.run()