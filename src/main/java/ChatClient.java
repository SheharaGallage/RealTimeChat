import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// --- Member 2's Main Task ---
// This class connects to the server and has two threads:
// 1. The main thread: Reads user input and sends it to the server.
// 2. A 'Listener' thread: Reads messages from the server and prints them.

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost"; // or an IP address
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {

        // --- Member 5's Task ---
        // To add encryption, Member 5 would change this 'Socket'
        // to an 'SSLSocket' using an SSLContext.
        // Socket socket = SslFactory.createSslSocket(SERVER_ADDRESS, SERVER_PORT);

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {

            System.out.println("Connected to the chat server!");

            // Setup streams to send/receive data
            // 'in' reads from the server, 'out' writes to the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // 'true' for auto-flush

            // 'consoleIn' reads from the user's keyboard
            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));

            // --- Member 2's Task (Multi-threading) ---
            // Start a new thread to listen for messages FROM the server
            // This is a simple way to create a thread using a lambda expression
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage); // Print server message
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server lost.");
                }
            });
            listenerThread.start(); // Start the listener thread

            // --- Main thread loop: Read user input and SEND to server ---
            String userInput;
            while (true) {
                userInput = consoleIn.readLine(); // Read from keyboard

                if (userInput == null) {
                    break; // Should not happen, but good to check
                }

                out.println(userInput); // Send the user's message to the server

                // If the user types /quit, they should also break their own loop
                if (userInput.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

            System.out.println("You have been disconnected.");

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
