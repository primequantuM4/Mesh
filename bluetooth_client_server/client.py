
import socket
import threading

def listen_for_messages(client_socket):
    """Thread function to listen for incoming messages."""
    
    while True:
        print("here")
        try:
            data = client_socket.recv(1024)  # Adjust buffer size if needed
            if not data:
                break
            print("Received:", data.decode('utf-8'))
        except Exception as e:
            print("Error in receiving:", e)
            break

def send_messages(client_socket):
    """Thread function to send messages."""
    while True:
        try:
            message = input("Enter message: ")
            client_socket.send(message.encode('utf-8'))
        except Exception as e:
            print("Error in sending:", e)
            break

def main():
    client = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
    server_address = ("18:56:80:F3:16:3B", 4)

    try:
        client.connect(server_address)
        print("Connected to the server.")
        
        # Start threads for listening and sending
        listener_thread = threading.Thread(target=listen_for_messages, args=(client,))
        listener_thread.start()
        send_messages(client)


        # Wait for both threads to finish
        listener_thread.join()
    except Exception as e:
        print("Connection error:", e)
    finally:
        client.close()
        print("Connection closed.")

if __name__ == "__main__":
    main()
