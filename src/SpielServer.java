import java.io.*;
import java.net.*;

public class SpielServer implements Runnable {
    private ServerSocket serverSocket; // für die Annahme von Client-Verbindungen
    private Socket clientSocket; // für die Verbindung zum verbundenen Client
    private PrintWriter out; // zum Senden von Ausgaben an den Client
    private BufferedReader in; // zum Empfangen von Eingaben vom Client
    private SpielSteuerung spielSteuerung;
    private boolean isRunning = false; // ob Server läuft
    private static final int PORT = 5000; // Port auf dem der Server auf Verbindung von Client lauscht 

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
            
            // Server in einem separaten Thread starten
            Thread thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            System.out.println("Fehler beim Starten des Servers: " + e.getMessage());
        }
    }

    /**
     * Server-Hauptschleife, die im separaten Thread läuft
     */
    @Override
    public void run() {
        try {
            // Auf Client-Verbindung warten
            System.out.println("Warte auf Client-Verbindung...");
            clientSocket = serverSocket.accept();
            System.out.println("Client verbunden: " + clientSocket.getInetAddress());

            // Input/Output Streams initialisieren
            // Der OutputStream des clientSocket wird verwendet, um Daten an den Client zu senden.
            // Der zweite Parameter 'true' aktiviert den AutoFlush-Modus, 
            // sodass der Puffer nach jedem Aufruf von z.B. println() automatisch geleert wird.
            // Dies stellt sicher, dass die gesendeten Daten sofort an den Client übertragen werden.
            out = new PrintWriter(clientSocket.getOutputStream(), true); 

            // Der InputStream des clientSocket wird verwendet, um Daten vom Client zu lesen.
            // InputStreamReader wandelt die Byte-Daten in Zeichen um, sodass sie als Text verarbeitet werden können.
            // BufferedReader ermöglicht das effiziente Lesen von Textdaten, indem er einen Puffer verwendet,
            // was die Leistung beim Lesen von Daten verbessert.
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Hauptschleife für die Kommunikation
            while (isRunning) {
                // Nachrichten vom Client empfangen
                String inputLine = in.readLine();
                if (inputLine != null) {
                    verarbeiteClientNachricht(inputLine);
                }
            }
        } catch (IOException e) {
            System.out.println("Verbindungsfehler: " + e.getMessage());
        } finally { // Der finally-Block wird immer ausgeführt, nachdem der try-Block und eventuelle catch-Blöcke abgeschlossen sind.
                    // In diesem Fall wird der Server gestoppt und alle Ressourcen freizugeben, unabhängig davon, ob im try-Block eine 
                    // Exception aufgetreten ist oder nicht.
                    // Dies stellt sicher, dass der Server immer korrekt heruntergefahren wird, um mögliche Ressourcenlecks zu vermeiden.
            stopServer();
        }
    }

    /**
     * Verarbeitet eingehende Nachrichten vom Client
     * @param nachricht Die empfangene Nachricht
     */
    private void verarbeiteClientNachricht(String nachricht) {
        // Format der Nachricht: BEFEHL:DATEN (Y-Position des 2.Spielers)
        String[] teile = nachricht.split(":");
    
        String befehl = teile[0];
        
        // Ohne dem würde die Pause-Nachricht beim Host nicht entfernt werden
        if (befehl.equals("VERSTECKE_NACHRICHT")) {
            // Verstecke die Pause-Nachricht beim Host
            spielSteuerung.versteckePauseNachricht();
            return;
        } else if (befehl.equals("MOVE")){
            int neuePosition = Integer.parseInt(teile[1]);

            if (neuePosition == -5) { // Spezielles Signal - Pause-Nachricht beim Host anzeigen (Spiel von Client pausiert)
                spielSteuerung.zeigePauseNachricht("Spieler 2 hat das Spiel pausiert");
                spielSteuerung.setPausiert(true);
            } else if (neuePosition == -1) { // Spezielles Signal - Neustart beim Host (Spiel von Client neugestartet)
                spielSteuerung.versteckePauseNachricht();
                spielSteuerung.spielNeustarten();
            } else if (neuePosition == -4) { // Spezielles Signal - Fortsetzen beim Host (Spiel von Client fortgesetzt)
                spielSteuerung.versteckePauseNachricht();
                spielSteuerung.fortsetzenSpiel();
            } else { // Normale Bewegung (von Spieler2)
                spielSteuerung.updateSpieler2Position(neuePosition);
            }
        }
    }

    /**
     * Sendet den Spielzustand an den Client
     * @param spielZustand Der aktuelle Spielzustand als String ("BEFEHL: ballX, ballY, spieler1Y, spieler2Y, spieler1Punkte, spieler2Punkte")
     */
    public void sendeSpielZustand(String spielZustand) {
        if (out != null) {
            // Bei FORTSETZEN oder NEUSTART auch die Pause-Nachricht entfernen
            // Sonst wird Pause-Nachricht beim Client nicht entfernt
            if (spielZustand.startsWith("FORTSETZEN:") || spielZustand.startsWith("NEUSTART:")) {
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
            System.out.println("Fehler beim Schließen des Servers: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob ein Client verbunden ist
     * @return true wenn ein Client verbunden ist, sonst false
     */
    public boolean istClientVerbunden() {
        return clientSocket != null && clientSocket.isConnected(); // isConnected (Klasse: Socket) - true wenn clientSocket mit Server verbunden
    }
}

