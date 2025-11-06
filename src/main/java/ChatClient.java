import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// --- Member 2's Main Task (Modified by Member 5) ---

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {

        try {
            // --- START: MEMBER 5'S TASK (Setup SSL) ---

            // 1. Create a "Trust All" TrustManager (for this assignment only)
            // This trusts our self-signed certificate
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            // 2. Set up the SSL Context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 3. Create the SSL Socket Factory
            SSLSocketFactory ssf = sslContext.getSocketFactory();

            // 4. Create the SSL Socket
            // (This replaces the old 'new Socket(SERVER_ADDRESS, SERVER_PORT)')
            try (Socket socket = ssf.createSocket(SERVER_ADDRESS, SERVER_PORT)) {

                System.out.println("Connected to the secure chat server!");
                // --- END: MEMBER 5'S TASK ---

                // --- (The rest of Member 2's code is identical) ---

                // Setup streams to send/receive data
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));

                // --- Member 2's Task (Multi-threading) ---
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

                // --- Main thread loop ---
                String userInput;
                while (true) {
                    userInput = consoleIn.readLine();
                    if (userInput == null) {
                        break;
                    }
                    out.println(userInput);
                    if (userInput.equalsIgnoreCase("/quit")) {
                        break;
                    }
                }
                System.out.println("You have been disconnected.");

            } // The SSLSocket is closed here

        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}