import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

// --- Member 1's Main Task (Modified by Member 5) ---

public class ChatServer {

    private static final int PORT = 1234;

    // --- START: MEMBER 5'S TASK (Keystore details) ---
    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();
    // --- END: MEMBER 5'S TASK ---

    public static void main(String[] args) {
        System.out.println("Chat Server is running and listening on port " + PORT + "...");

        try {
            // --- START: MEMBER 5'S TASK (Setup SSL) ---

            // 1. Load the Keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD);

            // 2. Set up the Key Manager Factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, KEYSTORE_PASSWORD);

            // 3. Set up the SSL Context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // 4. Create the SSL Server Socket Factory
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

            // 5. Create the SSL Server Socket
            // (This replaces the old 'new ServerSocket(PORT)')
            try (ServerSocket serverSocket = ssf.createServerSocket(PORT)) {

                System.out.println("SSL Server Socket created. Waiting for clients...");
                // --- END: MEMBER 5'S TASK ---

                // The main server loop (Member 1's logic)
                while (true) {
                    try {
                        // .accept() now provides an SSLSocket
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                        // --- Member 1's Task (Multi-threading) ---
                        ClientHandler clientThread = new ClientHandler(clientSocket);
                        clientThread.start();

                    } catch (IOException e) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            } // The SSLServerSocket is closed here

        } catch (Exception e) {
            // This will catch SSL errors, Keystore errors, etc.
            System.err.println("Server could not start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}