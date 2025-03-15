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
    private static final int SCHLAEGER_ABSTAND = 10; // Abstand des linken Schläger vom Spielfeldrand
    private static final int SCHLAEGER_GESCHWINDIGKEIT = 15;
    private static final long TASTENDRUCK_VERZOEGERUNG = 50; // 50ms Verzögerung zwischen Tastendrücken

    private int spieler1Y; // Y-Position des Schlägers des Spieler1
    private int spieler2Y; // Y-Position des Schlägers des Spieler2
    private int ballX; // X-Position des Balls
    private int ballY; // Y-Position des Balls
    private int ballXGeschwindigkeit; // Geschwindigkeit des Balls in x
    private int ballYGeschwindigkeit; // Geschwindigkeit des Balls in y
    private int spieler1Punkte = 0; // Punktestand des Spieler1
    private int spieler2Punkte = 0; // Punktestand des Spieler2
    private long letzterTastendruck = 0;
    private SpielModus modus; // Spielmodus
    private JFrame pausenMenueFrame; // Pausen-Menü
    private Thread spielThread; // Thread für Spiel
    private boolean spielLaeuft = true;
    public boolean istPausiert = false;
    private boolean istPausenMenueOffen = false;
    private SpielServer server;  // für Host
    private SpielClient client;  // für Client
    private boolean istHost;     // Unterscheidung zwischen Host und Client

    /**
     * Gemeinsamer Konstruktor für beide Modi
     */
    private SpielSteuerung(SpielFeld spielfeld, boolean istHost, String serverIP) {
        this.spielfeld = spielfeld;
        this.istHost = istHost;
        
        spielfeld.setFocusable(true);
        spieler1Y = spielfeld.getHeight() / 2 - SCHLAEGER_HOEHE / 2; // Mitte des Spielfelds
        spieler2Y = spielfeld.getHeight() / 2 - SCHLAEGER_HOEHE / 2; // Mitte des Spielfelds
        spieler1Punkte = 0;
        spieler2Punkte = 0;

        if (istHost) { // Ist Host
            server = new SpielServer(this); 
            server.startServer(); // Server starten
        } else { // Ist Client
            client = new SpielClient(this, serverIP);
            client.verbindeMitServer(); // Client-Verbindung starten
        }
    }

    /**
     * Vereinfachter Konstruktor für Host-Modus (Spieler 1)
     * @param spielfeld Das Spielfeld
     */
    public SpielSteuerung(SpielFeld spielfeld) {
        this(spielfeld, true, null);
    }

    /**
     * Vereinfachter Konstruktor für Client-Modus (Spieler 2)
     * @param spielfeld Das Spielfeld
     * @param serverIP Die IP-Adresse des Servers
     */
    public SpielSteuerung(SpielFeld spielfeld, String serverIP) {
        this(spielfeld, false, serverIP);
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
        
        // Sendet den Spielmodus an den Client, wenn der Host aktiv ist und ein Client verbunden ist.
        // Dies gewährleistet, dass beide Spieler im gleichen Modus spielen und synchronisiert sind.
        if (istHost && server != null && server.istClientVerbunden()) {
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
        
        // Spielfeld benachrichtigen, dass das Spiel gestartet wurde
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
            FontMetrics fm = g.getFontMetrics(); // um Informationen über die Schriftart zu erhalten
            int textWidth = fm.stringWidth(siegerText); // Breite des Textes (je nach Schriftart) holen
            int textHeight = fm.getHeight(); // Höhe des Textes (je nach Schriftart) holen
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
            // Wenn Host, wird die Spiellogik aktualisiert und der aktuelle Spielzustand an den verbundenen Client gesendet. 
            // Dies stellt sicher, dass der Client die neuesten Informationen über die Positionen der Spieler und den Ball erhält, 
            // um das Spiel synchron zu halten.
            if (istHost) {
                update();
                sendeSpielZustand(); // SpielZustand an Client senden
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
        // basierend auf Punktestand (am Anfang: fliegt der Ball immer nach rechts, danach: -1 -> links, 1 -> rechts)
        ballYGeschwindigkeit = Math.abs(ballYGeschwindigkeit); // Sicherstellen, dass der Ball korrekt startet (nach Unten)
    }

    /**
     * Verarbeitet Tastendrücke zur Steuerung der Schläger, zum Neustart bei Spielende und zu Anzeigen des Pause-Menü.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int taste = e.getKeyCode();
        long aktuelleZeit = System.currentTimeMillis();
        
        // Ob genug Zeit seit dem letzten Tastendruck vergangen ist
        // Ohne dem würden die Schläger-Bewegungen immer schneller werden, desto länger das Spiel geht
        if (aktuelleZeit - letzterTastendruck < TASTENDRUCK_VERZOEGERUNG) {
            return; // zu schnelle Tastendrücke ignorieren
        }
        
        letzterTastendruck = aktuelleZeit;

        // Gemeinsame Tastenfunktion für Host und Client
        if (taste == KeyEvent.VK_SPACE) {
            
            // Ob Spiel beendet ist (ein Spieler hat 3 Punkte)
            // Ohne dem könnte man auch wenn das Spiel fertig ist, das Spiel stoppen
            if (spieler1Punkte >= 3 || spieler2Punkte >= 3) {
                return;
            }
            
            pauseSpiel(); 
        }

        if (istHost) { // Spieler 1 Steuerung (nur für Host)
            if (taste == KeyEvent.VK_W && spieler1Y > 0 && spielLaeuft) {
                spieler1Y -= SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler1Y < 0){
                    spieler1Y = 0;
                }   
            }
            if (taste == KeyEvent.VK_S && spieler1Y < spielfeld.getHeight() - SCHLAEGER_HOEHE && spielLaeuft) {
                spieler1Y += SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler1Y > spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                    spieler1Y = spielfeld.getHeight() - SCHLAEGER_HOEHE;
                }
            }
        } else { // Spieler 2 Steuerung (nur für Client)
            if (taste == KeyEvent.VK_O && spieler2Y > 0 && spielLaeuft) {
                spieler2Y -= SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler2Y < 0){
                    spieler2Y = 0;
                } 
                client.sendeSpieler2Position(spieler2Y);
            }
            if (taste == KeyEvent.VK_L && spieler2Y < spielfeld.getHeight() - SCHLAEGER_HOEHE && spielLaeuft) {
                spieler2Y += SCHLAEGER_GESCHWINDIGKEIT;
                if (spieler2Y > spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                    spieler2Y = spielfeld.getHeight() - SCHLAEGER_HOEHE;
                }
                client.sendeSpieler2Position(spieler2Y);
            }
        }

        // Gemeinsame Tastenfuntkion für Host und CLient
        if (taste == KeyEvent.VK_ENTER && !spielLaeuft) {
            spielNeustarten();
        }
    }

    /**
     * Anhalten des Spiels und Anzeigen des Pause-Menüs
     */
    public void pauseSpiel() {    
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
        
        // Pause-Menü nur für den Spieler anzeigen, der pausiert hat
        pausenMenueAnzeigen();
        
        // Pause-Nachricht nur an den anderen Spieler senden
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
     * Neustarten des Spiels
     */
    public void spielNeustarten() {
        // Pause-Nachricht verstecken
        spielfeld.versteckePauseNachricht();
        
        // Spielzustand zurücksetzen
        spieler1Punkte = 0;
        spieler2Punkte = 0;
        ballZuruecksetzen();
        istPausiert = false;
        istPausenMenueOffen = false;
        
        // Alten Thread beenden falls vorhanden
        if (spielThread != null) {
            spielThread.interrupt();
            spielThread = null;
        }
        
        // Anderen Spieler benachrichtigen
        if (istHost) {
            server.sendeSpielZustand("NEUSTART:");
        } else {
            client.sendeSpieler2Position(-1); // -1 als Neustart-Signal
        }
        
        // Spiel neustarten
        spielLaeuft = true;
        spielThread = new Thread(this);
        spielThread.start();
        
        // Spielfeld richtig initialisieren
        spielfeld.spielGestartet();

        spielfeld.repaint();
    }

    /**
     * Fortsetzen des Spiels
     */
    public void fortsetzenSpiel() {
        if (pausenMenueFrame != null) {
            pausenMenueFrame.dispose();
        }
        
        // Pause-Nachricht verstecken
        spielfeld.versteckePauseNachricht();
        
        // Anderen Spieler benachrichtigen
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
                    
                    spielfeld.spielGestartet();
                }
            }
        });
        timer.start();
    }

    /**
     * Aktualisiert den Spielzustand basierend auf Netzwerknachrichten (wird vom Client aufgerufen)
     */
    public void updateSpielZustand(String zustand) {
        String[] parts = zustand.split(":"); // Format: BEFEHL:DATEN (ballx, bally, spieler1Y, spieler2Y, spieler1Punkte, spieler2Punkte)
        if (parts.length != 2) return;
        
        String daten = parts[1];
        String[] teile = daten.split(","); // Format: ballX,ballY,spieler1Y,spieler2Y,spieler1Punkte,spieler2Punkte
       
        if (teile.length == 6) {
            ballX = Integer.parseInt(teile[0]);
            ballY = Integer.parseInt(teile[1]);
            spieler1Y = Integer.parseInt(teile[2]);
            spieler2Y = Integer.parseInt(teile[3]);
            spieler1Punkte = Integer.parseInt(teile[4]);
            spieler2Punkte = Integer.parseInt(teile[5]);
                
            // Ob das Spiel beendet ist (ein Spieler hat 3 Punkte)
            if (spieler1Punkte >= 3 || spieler2Punkte >= 3) {
                spielLaeuft = false;
            } else {
                spielLaeuft = true;
            }   
            spielfeld.repaint();
        }
    }

    /**
     * Aktualisiert die Position von Spieler 2 (wird vom Server aufgerufen)
     */
    public void updateSpieler2Position(int position) {
        if (position == -5) { // Pause-Nachricht vom Client
            spielfeld.zeigePauseNachricht("Client hat das Spiel pausiert");
            istPausiert = true;
        } else if (position == -1) { // NEUSTART-Signal
            server.sendeSpielZustand("NEUSTART:");
            spielNeustarten();
        } else if (position == -2) { // PAUSE-Signal
            server.sendeSpielZustand("PAUSE:");
            pauseSpiel();
        } else if (position == -4) { // FORTSETZEN-Signal
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
        if (server != null && server.istClientVerbunden()) {
            String zustand = String.format("%d,%d,%d,%d,%d,%d",
                ballX, ballY, spieler1Y, spieler2Y, spieler1Punkte, spieler2Punkte); // Format der Zustands-Nachricht die an den Client gesendet wird
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

    /**
    * Überprüft, ob die SpielSteuerung (Host oder Client) verbunden ist.
    *
    * @return true, wenn der Host einen aktiven Server hat und ein Client verbunden ist,
    *         oder wenn der Client aktiv ist und mit dem Server verbunden ist; 
    *         andernfalls false.
    */
    public boolean istVerbunden() {
        if (istHost) {
            return server != null && server.istClientVerbunden();
        } else {
            return client != null && client.istVerbunden();
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

    /**
    * Setzt den Pausierungsstatus des Spiels.
    *
    * @param pausiert true, um das Spiel zu pausieren; false, um das Spiel fortzusetzen.
    */
    public void setPausiert(boolean pausiert) {
        this.istPausiert = pausiert;
    }
}
