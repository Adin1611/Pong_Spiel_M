import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Die Klasse SpielSteuerung steuert das Spiel (Spiellogik).
 */
public class SpielSteuerung extends KeyAdapter implements Runnable {
    private final SpielFeld spielfeld; // Spielfeld
    private static final int SCHLAEGER_BREITE = 20; // Breite der Schläger
    private static final int SCHLAEGER_HOEHE = 100; // Höhe der Schläger
    private static final int BALL_GROESSE = 20; // Durchmesser des Balls
    private static final int SCHLAEGER_ABSTAND = 10; // Abstand der Schläger vom Spielfeldrand
    private static final int SCHLAEGER_GESCHWINDIGKEIT = 15;
    private long letzterTastendruck = 0;
    private static final long TASTENDRUCK_VERZOEGERUNG = 50; // 50ms Verzögerung zwischen Tastendrücken

    private int spieler1Y; // Y-Position des Schlägers des Spieler1
    private int spieler2Y; // Y-Position des Schlägers des Spieler2
    private int ballX; // X-Position des Balls
    private int ballY; // Y-Position des Balls
    private int ballXGeschwindigkeit; // Geschwindigkeit des Balls in x
    private int ballYGeschwindigkeit; // Geschwindigkeit des Balls in y
    private int spieler1Punkte = 0; // Punktestand des Spieler1
    private int spieler2Punkte = 0; // Punktestand des Spieler2
    private SpielModus modus; // Spielmodus
    private JFrame pausenMenueFrame;
    private Thread spielThread; // Thread für die Spiel
    private boolean spielLaeuft = true;
    public boolean istPausiert = false;
    private boolean istPausenMenueOffen = false;
    private SpielServer server;  // Nur für Host
    private SpielClient client;  // Nur für Client
    private boolean istHost;     // Unterscheidung zwischen Host und Client
    private String serverIP;     // IP-Adresse für Client-Verbindung

    /**
     * Konstruktor für Host-Modus (Spieler 1)
     * @param spielfeld Das Spielfeld
     */
    public SpielSteuerung(SpielFeld spielfeld) {
        this(spielfeld, true, null);
    }

    /**
     * Konstruktor für Client-Modus (Spieler 2)
     * @param spielfeld Das Spielfeld
     * @param serverIP Die IP-Adresse des Servers
     */
    public SpielSteuerung(SpielFeld spielfeld, String serverIP) {
        this(spielfeld, false, serverIP);
    }

    /**
     * Gemeinsamer Konstruktor für beide Modi
     */
    private SpielSteuerung(SpielFeld spielfeld, boolean istHost, String serverIP) {
        this.spielfeld = spielfeld;
        this.istHost = istHost;
        this.serverIP = serverIP;
        
        spielfeld.setFocusable(true);
        spieler1Y = spielfeld.getHeight() / 2 - SCHLAEGER_HOEHE / 2;
        spieler2Y = spielfeld.getHeight() / 2 - SCHLAEGER_HOEHE / 2;
        spieler1Punkte = 0;
        spieler2Punkte = 0;

        if (istHost) {
            server = new SpielServer(this);
            server.startServer();
        } else {
            client = new SpielClient(this, serverIP);
            client.verbindeMitServer();
        }
    }

    /**
     * Berechnet dynamisch die x-Koordinate des rechten Schlägers auf Basis der Spielfeldbreite
     * @return x-Koordinate des Schlägers
     */
    private int rechterSchlaegerX() {
        return spielfeld.getWidth() - SCHLAEGER_ABSTAND - SCHLAEGER_BREITE;
    }

    /**
     * Setzt den Spielmodus und initialisiert die Spielparameter entsprechend.
     *
     * @param modus Der jeweilige Spielmodus.
     */
    public void setModus(SpielModus modus) {
        this.modus = modus;
        initialisiereModus();
        ballZuruecksetzen();
        
        // Sende den Modus an den Client, wenn wir der Host sind
        if (istHost && server != null && server.isClientVerbunden()) {
            server.sendeModus(modus);
        }
    }

    /**
     * Setzt den Spielmodus und startet das Spiel (wird vom Client aufgerufen)
     * @param modus Der Spielmodus
     */
    public void setModusUndStarteSpiel(SpielModus modus) {
        this.modus = modus;
        initialisiereModus();
        ballZuruecksetzen();
        
        // Starte das Spiel
        spielLaeuft = true;
        istPausiert = false;
        
        // Starte den Spielthread
        if (spielThread != null) {
            spielThread.interrupt();
        }
        spielThread = new Thread(this);
        spielThread.start();
        
        // Benachrichtige das Spielfeld, dass das Spiel gestartet wurde
        spielfeld.spielGestartet();
    }

    /**
     * Initialisiert die Ballgeschwindigkeit basierend auf dem aktuellen Spielmodus.
     */
    private void initialisiereModus() {
        switch (modus) {
            case EINFACH:
                ballXGeschwindigkeit = 2;
                ballYGeschwindigkeit = 2;
                break;
            case MITTEL:
                ballXGeschwindigkeit = 4;
                ballYGeschwindigkeit = 4;
                break;
            case SCHWER:
                ballXGeschwindigkeit = 6;
                ballYGeschwindigkeit = 6;
                break;
        }
    }

    /**
     * Zeichnet das Spielfeld und die Spielobjekte.
     *
     * @param g Das Graphics-Objekt zum Zeichnen.
     */
    public void zeichneSpielfeld(Graphics g) {
        if (spieler1Punkte >= 3 || spieler2Punkte >= 3) { // Spiel beenden, wenn ein Spieler 3 Punkte erreicht und Siegertext zeichnen
            spielLaeuft = false; // Spiel beenden

            // Siegertext und Restarttext zeichnen
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            String siegerText = spieler1Punkte >= 3 ? "Spieler 1 gewinnt!" : "Spieler 2 gewinnt!";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(siegerText);
            int textHeight = fm.getHeight();
            g.drawString(siegerText, (spielfeld.getWidth() - textWidth) / 2, (spielfeld.getHeight() - textHeight) / 2); // Zeichnet den Siegertext in der Mitte des Spielfelds

            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String restartText = "Drücke Enter, um neu zu starten";
            fm = g.getFontMetrics();
            textWidth = fm.stringWidth(restartText);
            g.drawString(restartText, (spielfeld.getWidth() - textWidth) / 2, (spielfeld.getHeight() - textHeight) / 2 + 50); // Zeichnet den Restarttext in der Mitte des Spielfelds, drawString(String str, int x, int y)
        }
        else { // Normales Spielfeld zeichnen
            // Mittellinie zeichnen
            g.setColor(Color.WHITE);
            g.drawLine(spielfeld.getWidth() / 2, 0, spielfeld.getWidth() / 2, spielfeld.getHeight()); // drawLine(int x1, int y1, int x2, int y2)

            // Kreis in der Mitte zeichnen
            int kreisDurchmesser = 150;
            int kreisX = spielfeld.getWidth() / 2 - kreisDurchmesser / 2;
            int kreisY = spielfeld.getHeight() / 2 - kreisDurchmesser / 2;
            g.drawOval(kreisX, kreisY, kreisDurchmesser, kreisDurchmesser); // 	drawOval(int x, int y, int width, int height)

            // Punkt in der Mitte des Kreises zeichnen
            int punktGroesse = 10;
            g.fillOval(spielfeld.getWidth() / 2 - punktGroesse / 2, spielfeld.getHeight() / 2 - punktGroesse / 2, punktGroesse, punktGroesse); // fillOval(int x, int y, int width, int height)

            // Schläger zeichnen
            g.fillRect(SCHLAEGER_ABSTAND, spieler1Y, SCHLAEGER_BREITE, SCHLAEGER_HOEHE); // fillRect(int x, int y, int width, int height)
            g.fillRect(rechterSchlaegerX(), spieler2Y, SCHLAEGER_BREITE, SCHLAEGER_HOEHE);


            // Ball zeichnen
            g.fillOval(ballX, ballY, BALL_GROESSE, BALL_GROESSE);

            // Punktestände zeichnen
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString(String.valueOf(spieler1Punkte), spielfeld.getWidth() / 2 - 50, 50);
            g.drawString(String.valueOf(spieler2Punkte), spielfeld.getWidth() / 2 + 30, 50);

            // Aktuellen Modus anzeigen
            g.setFont(new Font("Arial", Font.PLAIN, 20)); // Font.PLAIN - bedeutet,
            // dass der Text in normaler Schriftart (nicht fett, nicht kursiv) dargestellt wird.
            g.drawString("Modus: " + modus, spielfeld.getWidth() / 10, 20);
        }
    }

    /**
     * Hauptspielschleife, die das Spiel aktualisiert und pausiert.
     */
    @Override
    public void run() {
        spielLaeuft = true;
        spielThread = Thread.currentThread();
        
        while (spielLaeuft && !istPausiert) {
            if (istHost) {
                update();
                sendeSpielZustand();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    /**
     * Aktualisiert die Position des Balls und überprüft Kollisionen.
     */
    public void update() {
        if (spielLaeuft) {
            ballX += ballXGeschwindigkeit;
            ballY += ballYGeschwindigkeit;
            kollisionPruefen();
        }
    }

    /**
     * Prüft die Ballkollision mit den Wänden bzw. Schlägern
     */
    private void kollisionPruefen(){
        // Ballkollision mit oberer und unterer Wand
        if (ballY <= 0 || ballY >= spielfeld.getHeight() - BALL_GROESSE) {
            ballYGeschwindigkeit = -ballYGeschwindigkeit; // Richtung umkehren
        }

        // Ballkollision mit Schläger-Links (spieler1)
        if (ballX <= SCHLAEGER_ABSTAND + SCHLAEGER_BREITE && ballY + BALL_GROESSE >= spieler1Y && ballY <= spieler1Y + SCHLAEGER_HOEHE) {
            ballXGeschwindigkeit = Math.abs(ballXGeschwindigkeit);  // Ball nach rechts bewegen
        }
        else if (ballX <= 0) { // linke Wand berührt
            spieler2Punkte++;
            ballZuruecksetzen();
        }

        // Ballkollision mit Schläger-Rechts (spieler2)
        if (ballX + BALL_GROESSE >= rechterSchlaegerX() && ballY + BALL_GROESSE >= spieler2Y && ballY <= spieler2Y + SCHLAEGER_HOEHE) {
            ballXGeschwindigkeit = -Math.abs(ballXGeschwindigkeit);  // Ball nach links bewegen
        }
        else if (ballX >= spielfeld.getWidth() - BALL_GROESSE) { // rechte Wand berührt
            spieler1Punkte++;
            ballZuruecksetzen();
        }

        spielfeld.repaint(); // Spielfeld neu zeichnen
    }

    /**
     * Setzt die Ballposition zurück und bestimmt die Richtung basierend auf dem Punktestand.
     */
    private void ballZuruecksetzen() {
        ballX = spielfeld.getWidth() / 2 - BALL_GROESSE / 2; // Ball mittig in der x-Achse positionieren
        ballY = spielfeld.getHeight() / 2 - BALL_GROESSE / 2; // Ball mittig in der y-Achse positionieren
        ballXGeschwindigkeit = Math.abs(ballXGeschwindigkeit) * (spieler1Punkte > spieler2Punkte ? -1 : 1); // Richtung
        // basierend auf Punktestand (am Anfang: fliegt der Ball immer nach rechts, danach: -1: links, 1: rechts)
        ballYGeschwindigkeit = Math.abs(ballYGeschwindigkeit); // Sicherstellen, dass der Ball korrekt startet
    }

    /**
     * Verarbeitet Tastendrücke zur Steuerung der Schläger, zum Neustart bei Spielende und zu Anzeigen des Pause-Menü.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int taste = e.getKeyCode();
        long aktuelleZeit = System.currentTimeMillis();
        
        // Prüfe, ob genug Zeit seit dem letzten Tastendruck vergangen ist
        if (aktuelleZeit - letzterTastendruck < TASTENDRUCK_VERZOEGERUNG) {
            return; // Ignoriere zu schnelle Tastendrücke
        }
        
        letzterTastendruck = aktuelleZeit;

        // Gemeinsame Tastenfunktionen für Host und Client
        if (taste == KeyEvent.VK_SPACE) {
            // Debug-Ausgabe
            System.out.println("Leertaste gedrückt von " + (istHost ? "Host" : "Client"));
            System.out.println("spielLaeuft: " + spielLaeuft + ", istPausiert: " + istPausiert);
            
            // Forciere Pause unabhängig vom Status
            if (!istHost) {
                // Client sendet Pause-Signal an Server
                System.out.println("Client sendet Pause-Signal");
                client.sendeSpieler2Position(-2); // -2 als spezielles Signal für Pause
                pauseSpiel(); // Direkt pausieren, ohne Bedingung
            } else {
                pauseSpiel(); // Direkt pausieren, ohne Bedingung
            }
        }

        if (istHost) {
            // Spieler 1 Steuerung (nur für Host)
            if (taste == KeyEvent.VK_W && spieler1Y > 0) {
                spieler1Y -= SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler1Y < 0) spieler1Y = 0;
            }
            if (taste == KeyEvent.VK_S && spieler1Y < spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                spieler1Y += SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler1Y > spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                    spieler1Y = spielfeld.getHeight() - SCHLAEGER_HOEHE;
                }
            }
        } else {
            // Spieler 2 Steuerung (nur für Client)
            if (taste == KeyEvent.VK_O && spieler2Y > 0) {
                spieler2Y -= SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler2Y < 0) spieler2Y = 0;
                client.sendeSpieler2Position(spieler2Y);
            }
            if (taste == KeyEvent.VK_L && spieler2Y < spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                spieler2Y += SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler2Y > spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                    spieler2Y = spielfeld.getHeight() - SCHLAEGER_HOEHE;
                }
                client.sendeSpieler2Position(spieler2Y);
            }
        }

        if (!spielLaeuft && taste == KeyEvent.VK_ENTER) {
            if (istHost) {
                server.sendeSpielZustand("RESET:");
                spielNeustarten();
            } else {
                client.sendeSpieler2Position(-1); // Sende spezielle Position als Reset-Signal
            }
        }
    }

    /**
     * Anhalten des Spiels und Anzeigen des Pause-Menüs
     */
    public void pauseSpiel() {
        System.out.println("pauseSpiel() aufgerufen von " + (istHost ? "Host" : "Client"));
        System.out.println("istPausenMenueOffen: " + istPausenMenueOffen);
        
        // Forciere Pause unabhängig vom Status
        istPausiert = true;
        
        // Wenn der Spiel-Thread existiert und noch läuft, wird er unterbrochen
        if (spielThread != null && spielThread.isAlive()) {
            try {
                spielThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Zeige Pause-Menü nur für den Spieler an, der pausiert hat
        pausenMenueAnzeigen();
        
        // Sende Pause-Nachricht nur an den anderen Spieler
        if (istHost) {
            server.sendeSpielZustand("PAUSE_NACHRICHT:Spieler 1 hat das Spiel pausiert");
        } else {
            client.sendeSpieler2Position(-5); // -5 als Signal für Pause-Nachricht
        }
        
        istPausenMenueOffen = true;
    }

    /**
     * Zeigt eine Pause-Nachricht an
     */
    public void zeigePauseNachricht(String nachricht) {
        spielfeld.zeigePauseNachricht(nachricht);
    }

    /**
     * Anzeigen des Pausenmenüs
     */
    private void pausenMenueAnzeigen() {
        pausenMenueFrame = new JFrame("Pausenmenü");
        pausenMenueFrame.setSize(300, 200);
        pausenMenueFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pausenMenueFrame.setLocationRelativeTo(spielfeld);

        JButton neustartButton = new JButton("Neustart");
        neustartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pausenMenueFrame.dispose();
                istPausenMenueOffen = false;
                spielNeustarten();
            }
        });

        JButton fortsetzenButton = new JButton("Fortsetzen");
        fortsetzenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pausenMenueFrame.dispose();
                istPausenMenueOffen = false;
                fortsetzenSpiel();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        panel.add(neustartButton, gbc);
        gbc.gridy++;
        panel.add(fortsetzenButton, gbc);

        pausenMenueFrame.add(panel);
        pausenMenueFrame.setVisible(true);
    }

    /**
     * Zurückkehren zum Hauptmenü
     */
    public void zurueckZumHauptmenue() {
        // Spiel beenden und Verbindung trennen
        spielLaeuft = false;
        istPausiert = false;
        istPausenMenueOffen = false;
        
        // Thread beenden
        if (spielThread != null) {
            spielThread.interrupt();
            spielThread = null;
        }
        
        // Punktestände zurücksetzen
        spieler1Punkte = 0;
        spieler2Punkte = 0;
        
        // Benachrichtige den anderen Spieler
        if (istHost) {
            if (server != null) {
                server.sendeSpielZustand("HAUPTMENUE:");
                server.stopServer();
            }
        } else {
            if (client != null) {
                client.sendeSpieler2Position(-3); // -3 als Signal für Hauptmenü
                client.verbindungSchliessen();
            }
        }
        
        // Zum Hauptmenü zurückkehren
        spielfeld.zeigeSpielmodusAuswahl();  // Diese Methode muss public sein
        spielfeld.repaint();
    }

    /**
     * Neustarten des Spiels
     */
    public void spielNeustarten() {
        // Verstecke die Pause-Nachricht
        spielfeld.versteckePauseNachricht();
        
        // Setze Spielzustand zurück
        spieler1Punkte = 0;
        spieler2Punkte = 0;
        ballZuruecksetzen();
        istPausiert = false;
        istPausenMenueOffen = false;
        
        // Beende den alten Thread falls vorhanden
        if (spielThread != null) {
            spielThread.interrupt();
            spielThread = null;
        }
        
        // Benachrichtige den anderen Spieler
        if (istHost) {
            server.sendeSpielZustand("RESET:");
        } else {
            client.sendeSpieler2Position(-1); // -1 als Reset-Signal
        }
        
        // Starte das Spiel neu
        spielLaeuft = true;
        spielThread = new Thread(this);
        spielThread.start();
        
        // Stelle sicher, dass das Spielfeld richtig initialisiert ist
        spielfeld.spielGestartet();
        
        // Aktualisiere die Anzeige
        spielfeld.repaint();
        
        // Debug-Ausgabe
        System.out.println("Spiel neugestartet von " + (istHost ? "Host" : "Client"));
    }

    /**
     * Fortsetzen des Spiels
     */
    public void fortsetzenSpiel() {
        if (pausenMenueFrame != null) {
            pausenMenueFrame.dispose();
        }
        
        // Verstecke die Pause-Nachricht
        spielfeld.versteckePauseNachricht();
        
        // Benachrichtige den anderen Spieler
        if (istHost) {
            server.sendeSpielZustand("FORTSETZEN:");
        } else {
            client.sendeSpieler2Position(-4); // -4 als Signal für Fortsetzen
        }
        
        istPausenMenueOffen = false;
        countdownStarten();
    }

    /**
     * Fortsetzen des Spiels mit Countdown
     */
    private void countdownStarten() {
        Timer timer = new Timer(1000, new ActionListener() {
            int countdown = 3;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (countdown > 0) {
                    countdown--;
                } else {
                    ((Timer) e.getSource()).stop();
                    istPausiert = false;
                    // Spiel-Thread neu starten
                    if (spielThread != null) {
                        spielThread.interrupt();
                    }
                    spielThread = new Thread(SpielSteuerung.this);
                    spielThread.start();
                    
                    // Stelle sicher, dass das Spielfeld aktiv ist
                    spielfeld.spielGestartet();
                }
            }
        });
        timer.start();
    }

    /**
     * Aktualisiert den Spielzustand basierend auf Netzwerknachrichten
     */
    public void updateSpielZustand(String zustand) {
        String[] parts = zustand.split(":");
        if (parts.length != 2) return;
        
        String command = parts[0];
        String data = parts[1];
        
        if (command.equals("UPDATE")) {
            String[] teile = data.split(",");
            if (teile.length == 6) {
                ballX = Integer.parseInt(teile[0]);
                ballY = Integer.parseInt(teile[1]);
                spieler1Y = Integer.parseInt(teile[2]);
                spieler2Y = Integer.parseInt(teile[3]);
                spieler1Punkte = Integer.parseInt(teile[4]);
                spieler2Punkte = Integer.parseInt(teile[5]);
                spielfeld.repaint();
            }
        } else if (command.equals("MODUS")) {
            SpielModus modus = SpielModus.valueOf(data);
            setModusUndStarteSpiel(modus);
        } else if (command.equals("RESET")) {
            spielNeustarten();
        } else if (command.equals("PAUSE_NACHRICHT")) {
            // Nur Nachricht anzeigen, kein Pause-Menü
            zeigePauseNachricht(data);
            istPausiert = true;
        } else if (command.equals("FORTSETZEN")) {
            fortsetzenSpiel();
        } else if (command.equals("HAUPTMENUE")) {
            zurueckZumHauptmenue();
        }
    }

    /**
     * Aktualisiert die Position von Spieler 2 (wird vom Server aufgerufen)
     */
    public void updateSpieler2Position(int position) {
        if (position == -5) {
            // Pause-Nachricht vom Client
            spielfeld.zeigePauseNachricht("Client hat das Spiel pausiert");
            istPausiert = true;
        } else if (position == -1) {
            // Reset-Signal
            server.sendeSpielZustand("RESET:");
            spielNeustarten();
        } else if (position == -2) {
            // Pause-Signal
            server.sendeSpielZustand("PAUSE:");
            pauseSpiel();
        } else if (position == -3) {
            // Hauptmenü-Signal
            server.sendeSpielZustand("HAUPTMENUE:");
            zurueckZumHauptmenue();
        } else if (position == -4) {
            // Fortsetzen-Signal
            server.sendeSpielZustand("FORTSETZEN:");
            fortsetzenSpiel();
        } else {
            spieler2Y = position;
            spielfeld.repaint();
        }
    }

    /**
     * Sendet den aktuellen Spielzustand an den Client
     */
    private void sendeSpielZustand() {
        if (server != null && server.isClientVerbunden()) {
            String zustand = String.format("%d,%d,%d,%d,%d,%d",
                ballX, ballY, spieler1Y, spieler2Y, spieler1Punkte, spieler2Punkte);
            server.sendeSpielZustand("UPDATE:" + zustand);
        }
    }

    /**
     * Beendet das Spiel und schließt die Netzwerkverbindungen
     */
    public void beendeSpiel() {
        spielLaeuft = false;
        if (istHost && server != null) {
            server.stopServer();
        } else if (!istHost && client != null) {
            client.verbindungSchliessen();
        }
    }

    public boolean isVerbunden() {
        if (istHost) {
            return server != null && server.isClientVerbunden();
        } else {
            return client != null && client.isVerbunden();
        }
    }

    /**
     * Gibt zurück, ob der aktuelle Spieler der Host ist
     * @return true wenn der Spieler der Host ist, sonst false
     */
    public boolean istHost() {
        return istHost;
    }

    /**
     * Versteckt die Pause-Nachricht
     */
    public void versteckePauseNachricht() {
        spielfeld.versteckePauseNachricht();
    }

    public void setPausiert(boolean pausiert) {
        this.istPausiert = pausiert;
    }
}
