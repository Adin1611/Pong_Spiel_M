import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Die Klasse SpielFeld repräsentiert das Spielfeld für das Pong-Spiel.
 * Sie verwaltet die Benutzeroberfläche.
 */
public class SpielFeld extends JPanel {
    private SpielSteuerung steuerung; // Steuerung des Spiels
    private JButton einfachButton, mittelButton, schwerButton; // Buttons für die verschiedenen Schwierigkeitsgrade
    private JButton hostButton, clientButton; // Buttons für Host/Client-Auswahl
    private JTextField ipTextField; // Textfeld für Eingabe der IP-Adresse
    private JLabel titelLabel, infoLabel, verbindungsLabel; // Labels für Hauptmenü
    private Thread spielThread; // Thread für die Spielausführung, der die Spiellogik in einem separaten Thread ausführt
    private JFrame pauseNachrichtFrame; // Fenster wenn Spiel pausiert wird
    private boolean spielGestartet = false; // Status, ob das Spiel gestartet ist    

    /**
     * Konstruktor für das SpielFeld.
     * Initialisiert die Steuerung und die Benutzeroberfläche.
     */
    public SpielFeld() {
        setBackground(Color.BLACK); // Setzt den Hintergrund auf Schwarz
        setFocusable(true); // dadurch ist das Panel in der Lage, auf Tastatureingaben
        // zu reagieren, die zur Steuerung des Spiels verwendet werden. Ohne diese Einstellung
        // könnte das Panel keine Tastaturereignisse empfangen, und alle hinzugefügten
        // KeyListener würden nicht wie erwartet funktionieren.

        // Initialisierung der Labels
        titelLabel = new JLabel("Pong");
        titelLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titelLabel.setForeground(Color.WHITE); // Setzt die Schriftfarbe auf Weiß

        // in zeigeSpielmodusAuswahl() geaddet
        infoLabel = new JLabel("Wähle Spielmodus");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        infoLabel.setForeground(Color.WHITE);

        verbindungsLabel = new JLabel("");
        verbindungsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        verbindungsLabel.setForeground(Color.WHITE);

        // Initialisierung der Netzwerk-Komponenten
        hostButton = new JButton("Als Host spielen");
        clientButton = new JButton("Als Client verbinden");
        ipTextField = new JTextField(15);
        ipTextField.setToolTipText("Server-IP-Adresse eingeben");

        // Initialisierung der Spielmodus-Buttons
        einfachButton = new JButton("Einfach");
        mittelButton = new JButton("Mittel");
        schwerButton = new JButton("Schwer");

        // Host-Button ActionListener
        hostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e){
                steuerung = new SpielSteuerung(SpielFeld.this);
                zeigeSpielmodusAuswahl();
                verbindungsLabel.setText("Warte auf Client-Verbindung...");
                repaint();
                
                // Timer, der jede Sekunde (1000ms) überprüft, ob ein Client mit dem Host verbunden ist.
                // Sobald eine Verbindung hergestellt wurde, wird die Anzeige aktualisiert und der Timer gestoppt.
                Timer verbindungsTimer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (steuerung != null && steuerung.isVerbunden()) {
                            verbindungsLabel.setText("Client verbunden");
                            ((Timer) e.getSource()).stop();
                        }
                    }
                });
                verbindungsTimer.start();
                }
            }); 
            
        // Client-Button ActionListener
        clientButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                String ip = ipTextField.getText().trim(); // trim(): entfernt alle Leerzeichen, Tabulatoren und Zeilenumbrüche 
                                                          // am Anfang und Ende des Strings (IP-Adresse)
                if (ip.isEmpty()) {
                    JOptionPane.showMessageDialog(SpielFeld.this, "Bitte geben Sie eine IP-Adresse ein.");
                    return;
                }
                
                steuerung = new SpielSteuerung(SpielFeld.this, ip);
                versteckeButtonsLabels();
                repaint();
            
                // Dieser Timer überprüft jede Sekunde (1000ms), ob eine Verbindung zum Server hergestellt wurde.
                // Er wird gestartet, nachdem der Benutzer auf "Als Client verbinden" geklickt hat.
                // Sobald eine Verbindung hergestellt wurde, wird die Anzeige aktualisiert und der Timer gestoppt.
                Timer verbindungsTimer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (steuerung != null && steuerung.isVerbunden()) {
                            ((Timer) e.getSource()).stop();
                        }
                    }
                });
                verbindungsTimer.start();
            }
        });

        // Spielmodus-Button ActionListener
        einfachButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSpiel(SpielModus.EINFACH);
            }
        });
        mittelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                startSpiel(SpielModus.MITTEL);
            }
        });
        schwerButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                startSpiel(SpielModus.SCHWER);
            }
        });


        setLayout(new GridBagLayout()); // Setzt das Layout auf GridBagLayout (= organisiert Komponenten
        // in einem Raster, wobei jede Zelle anpassbar ist (z.B. Größe, Abstände, Positionierung).

        // Layout-Constraints für Zentrierung der Komponenten
        GridBagConstraints gbc = new GridBagConstraints(); // GridBagConstraints - definiert Regeln
        // für die Platzierung und Darstellung von Komponenten.
        gbc.gridx = 0; // gibt die Spalte an, in der die Komponente platziert wird (hier immer Spalte 0).
        gbc.gridy = 0; // gibt die Zeile an, in der die Komponente platziert wird (Start bei Zeile 0).
        gbc.insets = new Insets(10, 0, 10, 0); // Abstände (in Pixeln) um die
        // Komponente; Reihenfolge: oben, links, unten, rechts
        gbc.anchor = GridBagConstraints.CENTER; // zentriert die Komponente sowohl horizontal als auch
        // vertikal in der zugewiesenen Zelle

        // Hinzufügen der Komponenten zum Spielfeld
        add(titelLabel, gbc); // fügt eine Komponente unter Berücksichtigung der Constraints (Regeln) hinzu
        gbc.gridy++; // Nächste Zeile
        add(hostButton, gbc);
        gbc.gridy++;
        add(ipTextField, gbc);
        gbc.gridy++;
        add(clientButton, gbc);
        gbc.gridy++;
        add(verbindungsLabel, gbc);
    }

    /**
     * Zeigt die Spielmodus-Auswahl an
     */
    public void zeigeSpielmodusAuswahl() {
        removeAll(); // Vordefinierte Methode (aus Klasse: Container), die alle Komponenten aus dem Container entfernt

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        
        // Labels und Spielmodus-Buttons werden nur für den Host angezeigt
        add(titelLabel, gbc);
        gbc.gridy++;
        add(infoLabel, gbc);
        gbc.gridy++;
        add(einfachButton, gbc);
        gbc.gridy++;
        add(mittelButton, gbc);
        gbc.gridy++;
        add(schwerButton, gbc);
        gbc.gridy++;
        add(verbindungsLabel, gbc);
            
        einfachButton.setVisible(true);
        mittelButton.setVisible(true);
        schwerButton.setVisible(true);
        
        revalidate(); // Layout neu berechnen
        repaint(); // Panel neu zeichnen
    }

    /**
     * Versteckt alle Buttons und Labels
     */
    private void versteckeButtonsLabels() {
        hostButton.setVisible(false);
        clientButton.setVisible(false);
        ipTextField.setVisible(false);
        einfachButton.setVisible(false);
        mittelButton.setVisible(false);
        schwerButton.setVisible(false);
        titelLabel.setVisible(false);
        infoLabel.setVisible(false);
        verbindungsLabel.setVisible(false);
    }

    /**
     * Markiert das Spiel als gestartet und versteckt alle UI-Elemente
     */
    public void spielGestartet() {
        spielGestartet = true;
        versteckeButtonsLabels();
        
        // Alle KeyListener zuvor entfernen, um Duplikate zu vermeiden
        KeyListener[] listeners = getKeyListeners();
        for (KeyListener listener : listeners) {
            removeKeyListener(listener);
        }
        
        addKeyListener(steuerung); // Fügt den KeyListener hinzu
        requestFocusInWindow(); // requestFocusInWindow() fordert den Eingabefokus für diese Komponente an,
                                // Dies ist wichtig, damit die Komponente Tastatureingaben empfangen kann
                                // Ohne diesen Aufruf würden KeyListener nicht funktionieren, da die Komponente
                                // nicht im Fokus wäre und daher keine Tastaturereignisse erhalten würde
    }

    /**
     * Startet das Spiel im gewählten Modus
     */
    private void startSpiel(SpielModus modus) {
        spielGestartet = true;
        versteckeButtonsLabels();
        steuerung.setModus(modus);
        addKeyListener(steuerung);
        requestFocusInWindow();
        
        // Spielthread starten 
        spielThread = new Thread(steuerung);
        spielThread.start();
    }

    /**
     * Zeigt eine Pause-Nachricht in einer Box an, bei dem, der das Spiel nicht gestoppt hat 
     */
    public void zeigePauseNachricht(String nachricht) {
        if (pauseNachrichtFrame != null) {
            pauseNachrichtFrame.dispose();
        }
        
        pauseNachrichtFrame = new JFrame("Spiel pausiert");
        pauseNachrichtFrame.setSize(300, 100);
        pauseNachrichtFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pauseNachrichtFrame.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel nachrichtLabel = new JLabel(nachricht); // nachricht: "Spieler 1 hat das Spiel pausiert"
                                                       //       oder "Spieler 2 hat das Spiel pausiert"
        nachrichtLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(nachrichtLabel, gbc);

        pauseNachrichtFrame.add(panel);
        pauseNachrichtFrame.setVisible(true);
    }

    /**
     * Versteckt die Pause-Nachricht
     */
    public void versteckePauseNachricht() {
        if (pauseNachrichtFrame != null) {
            pauseNachrichtFrame.dispose();
            pauseNachrichtFrame = null;
        }
    }

    /**
     * Zeichnet das Spielfeld.
     *
     * @param g Das Graphics-Objekt zum Zeichnen.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // Zweck: Hintergrund der Komponente vor dem Zeichnen gelöscht wird,
                                 //  füllen des Hintergrunds mit der aktuellen Hintergrundfabreusw.
        if (spielGestartet) { // Zeichnet das Spielfeld, wenn das Spiel gestartet ist
            steuerung.zeichneSpielfeld(g); 
        }
    }
}

