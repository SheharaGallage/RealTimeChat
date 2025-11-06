import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// --- Member 3's Main Task ---
// This class runs on its own thread for each connected client.
// It handles reading messages from the client and broadcasting them to all clients.

public class ClientHandler extends Thread {

    private Socket clientSocket;
    private PrintWriter out; // For sending messages to this client
    private BufferedReader in; // For reading messages from this client
    private String clientName;

    // --- Member 3's Task (Thread-Safety) ---
    private static List<ClientHandler> allClients = Collections.synchronizedList(new ArrayList<>());

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Setup streams for communication
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true); // 'true' for auto-flush

            // --- First: Ask client for their name ---
            out.println("Welcome! Please enter your name:");
            clientName = in.readLine();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Guest" + (allClients.size() + 1);
            }
            // --- ADDED BY MEMBER 4: Check for duplicate names ---
            // (You can add logic here to prevent duplicate names)

            // --- Add this client to the shared list ---
            allClients.add(this);

            // --- Announce the new client to everyone ---
            System.out.println(clientName + " has joined the chat.");
            broadcastMessage("SERVER", clientName + " has joined the chat.");

            // --- Main message loop: Read messages from the client ---
            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {

                // --- Member 4's Task (Protocol) ---
                if (clientMessage.equalsIgnoreCase("/quit")) {
                    break; // Exit the loop to disconnect
                }
                // --- START: MEMBER 4'S NEW LOGIC ---
                else if (clientMessage.startsWith("/w ")) {
                    handlePrivateMessage(clientMessage);
                }
                else if (clientMessage.equalsIgnoreCase("/list")) {
                    handleListCommand();
                }
                // --- END: MEMBER 4'S NEW LOGIC ---
                else {
                    // --- Broadcast the message to all clients ---
                    broadcastMessage(clientName, clientMessage);
                }
            }

        } catch (IOException e) {
            System.err.println("Error in client handler (" + clientName + "): " + e.getMessage());
        } finally {
            // --- Clean up: This 'finally' block always runs ---

            // 1. Remove this client from the list
            allClients.remove(this);

            // 2. Announce that the client has left
            if (clientName != null) {
                System.out.println(clientName + " has left the chat.");
                broadcastMessage("SERVER", clientName + " has left the chat.");
            }

            // 3. Close the streams and socket
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client resources: " + e.getMessage());
            }
        }
    }

    // --- Member 3's Task (Broadcasting) ---
    private void broadcastMessage(String sender, String message) {
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                client.out.println(sender + ": " + message);
            }
        }
    }

    // --- START: MEMBER 4'S NEW METHODS ---

    /**
     * Handles the /list command by sending a list of all connected users
     * back to the client who requested it.
     */
    private void handleListCommand() {
        out.println("--- Currently Connected Users ---");
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                out.println("- " + client.clientName);
            }
        }
        out.println("---------------------------------");
    }

    /**
     * Handles a private message (e.g., "/w Bob Hello Bob!")
     * It parses the message and sends it only to the target user.
     */
    private void handlePrivateMessage(String message) {
        try {
            // Format: /w <targetName> <privateMessage>
            // Example: /w Bob Hello Bob, how are you?
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                out.println("SERVER: Invalid private message. Use: /w <username> <message>");
                return;
            }

            String targetName = parts[1];
            String privateMessage = parts[2];

            // Find the target client and send the message
            boolean foundUser = false;
            synchronized (allClients) {
                for (ClientHandler client : allClients) {
                    if (client.clientName.equalsIgnoreCase(targetName)) {
                        // Send to the target user
                        client.out.println("[Private from " + this.clientName + "]: " + privateMessage);
                        foundUser = true;
                        break;
                    }
                }
            }

            if (foundUser) {
                // Also send a copy to the sender so they see their message
                out.println("[Private to " + targetName + "]: " + privateMessage);
            } else {
                out.println("SERVER: User '" + targetName + "' not found or is offline.");
            }

        } catch (Exception e) {
            out.println("SERVER: Error processing private message.");
        }
    }

    // --- END: MEMBER 4'S NEW METHODS ---
}