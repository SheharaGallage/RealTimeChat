import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
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
                            // Special handling for incoming images: /imgfrom <sender> <filename> <base64>
                            if (serverMessage.startsWith("/imgfrom ")) {
                                String[] parts = serverMessage.split(" ", 4);
                                if (parts.length >= 4) {
                                    String sender = parts[1];
                                    String filename = parts[2];
                                    String base64 = parts[3];
                                    try {
                                        byte[] data = Base64.getDecoder().decode(base64);
                                        Path downloads = Paths.get("downloads");
                                        if (!Files.exists(downloads)) {
                                            Files.createDirectories(downloads);
                                        }
                                        String outName = System.currentTimeMillis() + "_" + filename;
                                        Path outPath = downloads.resolve(outName);
                                        Files.write(outPath, data);
                                        System.out.println(
                                                "IMAGE from " + sender + ": saved to " + outPath.toAbsolutePath());
                                        // Attempt to open the image automatically
                                        try {
                                            if (Desktop.isDesktopSupported()) {
                                                Desktop.getDesktop().open(outPath.toFile());
                                            } else {
                                                String os = System.getProperty("os.name").toLowerCase();
                                                if (os.contains("win")) {
                                                    new ProcessBuilder("cmd", "/c", "start", "\"\"",
                                                            outPath.toAbsolutePath().toString()).start();
                                                } else if (os.contains("mac")) {
                                                    new ProcessBuilder("open", outPath.toAbsolutePath().toString())
                                                            .start();
                                                } else {
                                                    new ProcessBuilder("xdg-open", outPath.toAbsolutePath().toString())
                                                            .start();
                                                }
                                            }
                                        } catch (Exception openEx) {
                                            System.out.println("(Could not open image automatically)");
                                        }

                                    } catch (IllegalArgumentException iae) {
                                        System.out.println("SERVER: Received malformed image data from " + parts[1]);
                                    }
                                } else {
                                    System.out.println(serverMessage);
                                }
                            } else {
                                System.out.println(serverMessage); // Print server message
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Connection to server lost.");
                    }
                });
                listenerThread.start(); // Start the listener thread

                System.out.println(in.readLine()); // SERVER: Welcome! Please enter your name:

                // --- Ask for your name first ---
                System.out.print("Enter your name: ");
                String name = consoleIn.readLine();
                if (name == null || name.trim().isEmpty()) {
                    name = "Guest"; // default if user enters nothing
                }
                out.println(name); // send name to server

                // Announce presence to the server
                out.println("/status online");

                System.out.println("Connected as " + name + ". Available commands:");
                System.out.println("/quit, /list, /w <user> <msg>, /status <online|away|offline>");
                System.out.println("/typing <start|stop>, /me <action>, /lastseen <user>");

                // --- Main thread loop ---
                String userInput;
                while (true) {
                    userInput = consoleIn.readLine();
                    if (userInput == null) {
                        break;
                    }
                    // Image send shortcut: /img <local-path>
                    if (userInput.startsWith("/img ")) {
                        String pathStr = userInput.substring(5).trim();
                        Path p = Paths.get(pathStr);
                        try {
                            if (!Files.exists(p) || Files.isDirectory(p)) {
                                System.out.println("Local file not found: " + pathStr);
                                continue;
                            }
                            long size = Files.size(p);
                            long maxBytes = 5L * 1024 * 1024; // 5MB
                            if (size > maxBytes) {
                                System.out.println("File too large. Max 5MB allowed.");
                                continue;
                            }
                            String filename = p.getFileName().toString();
                            byte[] bytes = Files.readAllBytes(p);
                            String b64 = Base64.getEncoder().encodeToString(bytes);
                            out.println("/img " + filename + " " + b64);
                            System.out.println("Sent image: " + filename);
                        } catch (IOException ex) {
                            System.out.println("Error reading file: " + ex.getMessage());
                        }
                        continue;
                    }
                    // Provide a small shortcut for away/online
                    if (userInput.equalsIgnoreCase("/away")) {
                        userInput = "/status away";
                    } else if (userInput.equalsIgnoreCase("/online")) {
                        userInput = "/status online";
                    }

                    // Send and handle quit to notify server we are going offline
                    if (userInput.equalsIgnoreCase("/quit")) {
                        out.println("/status offline");
                        out.println(userInput);
                        break;
                    }

                    out.println(userInput);
                }
                System.out.println("You have been disconnected.");

            } // The SSLSocket is closed here

        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}