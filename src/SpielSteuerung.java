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
 * Die Klasse SpielSteuerung steuert das Spiel, indem sie die Bewegungen der Schläger und des Balls verwaltet
 * und die Spielregeln implementiert.
 */
public class SpielSteuerung extends KeyAdapter implements Runnable {
    private final SpielFeld spielfeld; // Spielfeld
    private static final int SCHLAEGER_BREITE = 20; // Breite der Schläger
    private static final int SCHLAEGER_HOEHE = 100; // Höhe der Schläger
    private static final int BALL_GROESSE = 20; // Durchmesser des Balls
    private static final int SCHLAEGER_ABSTAND = 10; // Abstand der Schläger vom Spielfeldrand

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
    private boolean istPausiert = false;
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
        if (istHost) {
            server.sendeModus(modus);
        }
        initialisiereModus();
        ballZuruecksetzen();
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
     *
     * @param e Das KeyEvent, das die gedrückte Taste beschreibt.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int taste = e.getKeyCode();

        if (istHost) {
            // Spieler 1 Steuerung (nur für Host)
            if (taste == KeyEvent.VK_W && spieler1Y > 0) {
                spieler1Y -= 15;
            }
            if (taste == KeyEvent.VK_S && spieler1Y < spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                spieler1Y += 15;
            }
        } else {
            // Spieler 2 Steuerung (nur für Client)
            if (taste == KeyEvent.VK_UP && spieler2Y > 0) {
                spieler2Y -= 15;
                client.sendeSpieler2Position(spieler2Y);
            }
            if (taste == KeyEvent.VK_DOWN && spieler2Y < spielfeld.getHeight() - SCHLAEGER_HOEHE) {
                spieler2Y += 15;
                client.sendeSpieler2Position(spieler2Y);
            }
        }

        // Gemeinsame Tastenfunktionen
        if (!spielLaeuft && taste == KeyEvent.VK_ENTER) {
            if (istHost) {
                server.sendeSpielZustand("RESET");
            }
            zurueckZumHauptmenue();
        }

        if (spielLaeuft && taste == KeyEvent.VK_SPACE) {
            pauseSpiel();
        }
    }

    /**
     * Anhalten des Spiels und Anzeigen des Pause-Menüs
     */
    private void pauseSpiel() {
        if (!istPausenMenueOffen) {
            istPausiert = true;
            // Wenn der Spiel-Thread existiert und noch läuft, wird er unterbrochen
            if (spielThread != null && spielThread.isAlive()) {
                try {
                    spielThread.interrupt(); // unterbricht den laufenden Spiel-Thread
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            pausenMenueAnzeigen();
            istPausenMenueOffen = true;
        }
    }

    /**
     * Anzeigen des Pausenmenüs
     */
    private void pausenMenueAnzeigen() {
        pausenMenueFrame = new JFrame("Pausenmenü");
        pausenMenueFrame.setSize(300, 200);
        pausenMenueFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pausenMenueFrame.setLocationRelativeTo(spielfeld);

        JButton hauptmenueButton = new JButton("Hauptmenü");
        hauptmenueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pausenMenueFrame.dispose(); // schließt das Pause-Menü
                istPausenMenueOffen = false;
                zurueckZumHauptmenue();
            }
        });

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
        panel.setLayout(new GridBagLayout()); // Layout auf GridBagLayout setzen
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; // Spalte
        gbc.gridy = 0; // Zeile
        gbc.insets = new Insets(10, 0, 10, 0); // Abstand zwischen den Buttons
        gbc.anchor = GridBagConstraints.CENTER; // Zentrieren

        panel.add(hauptmenueButton, gbc); // Hauptmenü Button hinzufügen
        gbc.gridy++; // Nächste Zeile

        panel.add(neustartButton, gbc); // Neustart Button hinzufügen
        gbc.gridy++; // Nächste Zeile

        panel.add(fortsetzenButton, gbc); // Fortsetzen Button hinzufügen

        pausenMenueFrame.add(panel);
        pausenMenueFrame.setVisible(true);
    }

    /**
     * Zurückkehren zum Hauptmenü
     */
    public void zurueckZumHauptmenue() {
        spieler1Punkte = 0;
        spieler2Punkte = 0;
        spielfeld.resetSpiel();
        spielfeld.repaint();
    }

    /**
     * Neustarten des Spiels
     */
    private void spielNeustarten() {
        spieler1Punkte = 0;
        spieler2Punkte = 0;
        ballZuruecksetzen();

        // Spiel-Thread neu starten (damit das Spiel dann wieder läuft, wenn der Thread nicht neu
        // gestartet wird, würde die Spiellogik nicht mehr ausgeführt werden)
        spielThread = new Thread(this); // erstellt einen neuen Thread, der die run()-Methode
        // des aktuellen Objekts (this) ausführt
        spielThread.start();
    }

    /**
     * Fortsetzen des Spiels
     */
    private void fortsetzenSpiel() {
        countdownStarten();
        istPausiert = false;
        if (pausenMenueFrame != null) {
            pausenMenueFrame.dispose(); // Menü schließen
        }
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
                    // System.out.println(countdown);
                    countdown--;
                } else {
                    ((Timer) e.getSource()).stop(); // stoppt den Timer
                    // Spiel-Thread neu starten - gilt das Gleiche wie vorher
                    spielThread = new Thread(SpielSteuerung.this);
                    spielThread.start();
                }
            }
        });
        timer.start();
    }

    /**
     * Aktualisiert den Spielzustand basierend auf Netzwerknachrichten
     */
    public void updateSpielZustand(String zustand) {
        // Format: "ballX,ballY,spieler1Y,spieler2Y,spieler1Punkte,spieler2Punkte"
        String[] teile = zustand.split(",");
        ballX = Integer.parseInt(teile[0]);
        ballY = Integer.parseInt(teile[1]);
        spieler1Y = Integer.parseInt(teile[2]);
        spieler2Y = Integer.parseInt(teile[3]);
        spieler1Punkte = Integer.parseInt(teile[4]);
        spieler2Punkte = Integer.parseInt(teile[5]);
        spielfeld.repaint();
    }

    /**
     * Aktualisiert die Position von Spieler 2 (wird vom Server aufgerufen)
     */
    public void updateSpieler2Position(int position) {
        spieler2Y = position;
        spielfeld.repaint();
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
}
