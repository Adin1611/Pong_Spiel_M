import java.io.*;
import java.net.*;

public class SpielClient implements Runnable {
    private Socket socket; // für die Verbindung zum Server
    private PrintWriter out; // zum Senden von Ausgbane an den Server
    private BufferedReader in; // zum Empfangen von Eingaben vom Sever
    private SpielSteuerung spielSteuerung;
    private boolean isRunning = false; // ob Client aktiv läuft
    private String serverIP; // IP-Adresse des Servers
    private static final int PORT = 5000; // Port auf dem Server auf Verbindung lauscht

    /**
     * Konstruktor für den SpielClient
     * @param spielSteuerung Die Spielsteuerung des Clients
     * @param serverIP Die IP-Adresse des Servers
     */
    public SpielClient(SpielSteuerung spielSteuerung, String serverIP) {
        this.spielSteuerung = spielSteuerung;
        this.serverIP = serverIP;
    }

    /**
     * Verbindet den Client mit dem Server
     */
    public void verbindeMitServer() {
        try {
            socket = new Socket(serverIP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning = true;
            
            // Client in einem separaten Thread starten
            Thread thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            System.out.println("Verbindungsfehler: " + e.getMessage());
        }
    }

    /**
     * Client-Hauptschleife, die im separaten Thread läuft
     */
    @Override
    public void run() {
        try {
            while (isRunning) {
                // Nachrichten vom Server empfangen
                String inputLine = in.readLine();
                if (inputLine != null) {
                    verarbeiteServerNachricht(inputLine);
                }
            }
        } catch (IOException e) {
            System.out.println("Verbindungsfehler: " + e.getMessage());
        } finally {
            verbindungSchliessen();
        }
    }

    /**
     * Verarbeitet eingehende Nachrichten vom Server
     * @param nachricht Die empfangene Nachricht
     */
    private void verarbeiteServerNachricht(String nachricht) {
        String[] teile = nachricht.split(":");
        if (teile.length < 1) return;

        String befehl = teile[0];
        
        // Ohne dem würde die Pause-Nachricht beim Client nicht entfernt werden
        if (befehl.equals("VERSTECKE_NACHRICHT")) {
            spielSteuerung.versteckePauseNachricht();
            return;
        }
        
        if (teile.length < 2) return;

        String daten = teile[1];
        
        switch (befehl) {
            case "MODUS": // Modus (der vom Host gewählt wurde) beim Client setzten
                SpielModus modus = SpielModus.valueOf(daten);
                spielSteuerung.setModusUndStarteSpiel(modus);
                break;
            case "UPDATE": // Spielstand updaten
                spielSteuerung.updateSpielZustand(nachricht);
                break;
            case "NEUSTART": // Spiel neustarten
                spielSteuerung.spielNeustarten();
                break;
            case "PAUSE": // Spiel pausieren
                spielSteuerung.pauseSpiel();
                break;
            case "FORTSETZEN": // Spiel pausieren
                spielSteuerung.fortsetzenSpiel();
                break;
            case "PAUSE_NACHRICHT": // Pause-Nachricht beim Client anzeigen 
                spielSteuerung.zeigePauseNachricht(daten);
                break;
        }
    }

    /**
     * Sendet die Position des Spieler 2 an den Server
     * @param position Die Y-Position des Schlägers
     */
    public void sendeSpieler2Position(int position) {
        if (out != null) {
            // Spezielles Signal
            if (position == -5) { // Spezielles Signal - Pause-Nachricht (Spiel von Client pausiert)
                out.println("MOVE:" + position);
            } else if (position == -4 || position == -1) { // Spezielles Signal - Fortsetzen oder Neustart (Spiel von Client fortgesetzt bzw. neugestartet)
                                                           // Erst Verstecken-Signal senden, sonst würde das PausenMenü beim Host nicht geschlossen werden
                out.println("VERSTECKE_NACHRICHT:");
                // Dann das eigentliche Signal
                out.println("MOVE:" + position);
            } else { // Normale Bewegung (von Spieler2)
                out.println("MOVE:" + position);
            }
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
            System.out.println("Fehler beim Schließen der Verbindung: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob der Client mit dem Server verbunden ist
     * @return true wenn verbunden, sonst false
     */
    public boolean istVerbunden() {
        return socket != null && socket.isConnected();
    }
}

