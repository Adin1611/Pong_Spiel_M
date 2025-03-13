import java.io.*;
import java.net.*;

public class SpielServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private SpielSteuerung spielSteuerung;
    private boolean isRunning = false;
    private static final int PORT = 5000;

    /**
     * Konstruktor für den SpielServer
     * @param spielSteuerung Die Spielsteuerung des Host-Spielers
     */
    public SpielServer(SpielSteuerung spielSteuerung) {
        this.spielSteuerung = spielSteuerung;
    }

    /**
     * Startet den Server und wartet auf eine Client-Verbindung
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Server gestartet auf Port " + PORT);
            
            // Starte den Server in einem separaten Thread
            new Thread(this).start();
        } catch (IOException e) {
            System.err.println("Fehler beim Starten des Servers: " + e.getMessage());
        }
    }

    /**
     * Server-Hauptschleife, die im separaten Thread läuft
     */
    @Override
    public void run() {
        try {
            // Warte auf Client-Verbindung
            System.out.println("Warte auf Client-Verbindung...");
            clientSocket = serverSocket.accept();
            System.out.println("Client verbunden: " + clientSocket.getInetAddress());

            // Initialisiere Input/Output Streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Hauptschleife für die Kommunikation
            while (isRunning) {
                // Empfange Nachrichten vom Client
                String inputLine = in.readLine();
                if (inputLine != null) {
                    verarbeiteClientNachricht(inputLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Verbindungsfehler: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    /**
     * Verarbeitet eingehende Nachrichten vom Client
     * @param nachricht Die empfangene Nachricht
     */
    private void verarbeiteClientNachricht(String nachricht) {
        // Format der Nachricht: COMMAND:DATA
        String[] parts = nachricht.split(":");
        String command = parts[0];

        if (command.equals("VERSTECKE_NACHRICHT")) {
            // Verstecke die Pause-Nachricht beim Host
            spielSteuerung.versteckePauseNachricht();
            return;
        }

        switch (command) {
            case "MOVE":
                int neuePosition = Integer.parseInt(parts[1]);
                if (neuePosition == -5) {
                    // Pause-Nachricht vom Client - nur Nachricht anzeigen, kein Pause-Menü
                    spielSteuerung.zeigePauseNachricht("Spieler 2 hat das Spiel pausiert");
                    spielSteuerung.setPausiert(true);
                } else if (neuePosition == -1) {
                    // Reset-Signal
                    spielSteuerung.versteckePauseNachricht();
                    spielSteuerung.spielNeustarten();
                    sendeSpielZustand("RESET:");
                } else if (neuePosition == -2) {
                    // Pause-Signal - nicht mehr verwendet
                } else if (neuePosition == -4) {
                    // Fortsetzen-Signal
                    spielSteuerung.versteckePauseNachricht();
                    spielSteuerung.fortsetzenSpiel();
                    sendeSpielZustand("FORTSETZEN:");
                } else {
                    // Normale Bewegung
                    spielSteuerung.updateSpieler2Position(neuePosition);
                }
                break;
            // Weitere Befehle können hier hinzugefügt werden
        }
    }

    /**
     * Sendet den Spielzustand an den Client
     * @param spielZustand Der aktuelle Spielzustand als String
     */
    public void sendeSpielZustand(String spielZustand) {
        if (out != null) {
            // Bei FORTSETZEN oder RESET auch die Pause-Nachricht entfernen
            if (spielZustand.startsWith("FORTSETZEN:") || spielZustand.startsWith("RESET:")) {
                out.println("VERSTECKE_NACHRICHT:");
            }
            out.println(spielZustand);
        }
    }

    /**
     * Sendet den ausgewählten Spielmodus an den Client
     * @param modus Der gewählte SpielModus
     */
    public void sendeModus(SpielModus modus) {
        if (out != null) {
            out.println("MODUS:" + modus.name());
        }
    }

    /**
     * Stoppt den Server und schließt alle Verbindungen
     */
    public void stopServer() {
        isRunning = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Fehler beim Schließen des Servers: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob ein Client verbunden ist
     * @return true wenn ein Client verbunden ist, sonst false
     */
    public boolean isClientVerbunden() {
        return clientSocket != null && clientSocket.isConnected();
    }
}

