import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// --- Member 1's Main Task ---
// This class listens for client connections and passes them to a new thread.

public class ChatServer {

    private static final int PORT = 1234; // The port the server listens on

    public static void main(String[] args) {
        System.out.println("Chat Server is running and listening on port " + PORT + "...");

        // --- Member 5's Task ---
        // To add encryption, Member 5 would change this 'ServerSocket'
        // to an 'SSLServerSocket' using an SSLContext and a Keystore.
        // ServerSocket serverSocket = SslFactory.createSslServerSocket(PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // The main server loop. It waits for a client to connect.
            while (true) {
                try {
                    // .accept() blocks (waits) until a client connects.
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                    // --- Member 1's Task (Multi-threading) ---
                    // Create a new thread (ClientHandler) to handle this client.
                    // This allows the server to handle multiple clients at once.
                    ClientHandler clientThread = new ClientHandler(clientSocket);
                    clientThread.start(); // This calls the run() method in ClientHandler

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Server could not start on port " + PORT + ": " + e.getMessage());
        }
    }
}