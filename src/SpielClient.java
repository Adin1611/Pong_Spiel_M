import java.io.*;
import java.net.*;

public class SpielClient implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private SpielSteuerung spielSteuerung;
    private boolean isRunning = false;
    private String serverIP;
    private static final int PORT = 5000;

    /**
     * Konstruktor für den SpielClient
     * @param spielSteuerung Die Spielsteuerung des Client-Spielers
     * @param serverIP Die IP-Adresse des Servers
     */
    public SpielClient(SpielSteuerung spielSteuerung, String serverIP) {
        this.spielSteuerung = spielSteuerung;
        this.serverIP = serverIP;
    }

    /**
     * Verbindet den Client mit dem Server
     * @return true wenn die Verbindung erfolgreich hergestellt wurde, sonst false
     */
    public boolean verbindeMitServer() {
        try {
            socket = new Socket(serverIP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning = true;
            
            // Starte den Client in einem separaten Thread
            new Thread(this).start();
            return true;
        } catch (IOException e) {
            System.err.println("Verbindungsfehler: " + e.getMessage());
            return false;
        }
    }

    /**
     * Client-Hauptschleife, die im separaten Thread läuft
     */
    @Override
    public void run() {
        try {
            while (isRunning) {
                // Empfange Nachrichten vom Server
                String inputLine = in.readLine();
                if (inputLine != null) {
                    verarbeiteServerNachricht(inputLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Verbindungsfehler: " + e.getMessage());
        } finally {
            verbindungSchliessen();
        }
    }

    /**
     * Verarbeitet eingehende Nachrichten vom Server
     * @param nachricht Die empfangene Nachricht
     */
    private void verarbeiteServerNachricht(String nachricht) {
        // Format der Nachricht: COMMAND:DATA
        String[] parts = nachricht.split(":");
        if (parts.length < 2) return;
        
        String command = parts[0];

        switch (command) {
            case "MODUS":
                // Setze den Spielmodus und starte das Spiel automatisch
                SpielModus modus = SpielModus.valueOf(parts[1]);
                spielSteuerung.setModusUndStarteSpiel(modus);
                break;
            case "UPDATE":
                // Aktualisiere Spielzustand (Ball, Spieler 1, Punkte)
                spielSteuerung.updateSpielZustand(nachricht);
                break;
            case "RESET":
                // Zurück zum Hauptmenü
                spielSteuerung.zurueckZumHauptmenue();
                break;
        }
    }

    /**
     * Sendet die Position des Spieler 2 an den Server
     * @param position Die Y-Position des Schlägers
     */
    public void sendeSpieler2Position(int position) {
        if (out != null) {
            out.println("MOVE:" + position);
        }
    }

    /**
     * Schließt die Verbindung zum Server
     */
    public void verbindungSchliessen() {
        isRunning = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Fehler beim Schließen der Verbindung: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob der Client mit dem Server verbunden ist
     * @return true wenn verbunden, sonst false
     */
    public boolean isVerbunden() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}

