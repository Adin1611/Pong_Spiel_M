import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Die Klasse SpielFeld repräsentiert das Spielfeld für das Pong-Spiel.
 * Sie verwaltet die Benutzeroberfläche und die Spielsteuerung.
 */
public class SpielFeld extends JPanel {
    private SpielSteuerung steuerung; // Steuerung des Spiels
    private JButton einfachButton, mittelButton, schwerButton; // Buttons für die verschiedenen Schwierigkeitsgrade
    private JButton hostButton, clientButton;  // Neue Buttons für Host/Client-Auswahl
    private JTextField ipTextField;            // Textfeld für IP-Eingabe
    private JLabel titelLabel, infoLabel, verbindungsLabel;
    private Thread spielThread; // Thread für die Spielausführung, der die Spiellogik in einem separaten
    // Thread ausführt
    private boolean spielGestartet = false; // Status, ob das Spiel gestartet ist
    private boolean netzwerkAuswahlAnzeigen = true;  // Neue Variable für den Auswahlzustand
    private JFrame pauseNachrichtFrame;

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
        hostButton.addActionListener(e -> {
            netzwerkAuswahlAnzeigen = false;
            steuerung = new SpielSteuerung(this);
            zeigeSpielmodusAuswahl();
            verbindungsLabel.setText("Warte auf Client-Verbindung...");
            repaint();
            
            // Starte einen Timer, der den Verbindungsstatus überprüft
            Timer verbindungsTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (steuerung != null && steuerung.isVerbunden()) {
                        verbindungsLabel.setText("Client verbunden");
                        ((Timer)evt.getSource()).stop();
                    }
                }
            });
            verbindungsTimer.start();
        });

        // Client-Button ActionListener
        clientButton.addActionListener(e -> {
            String ip = ipTextField.getText().trim();
            if (ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bitte geben Sie eine IP-Adresse ein.");
                return;
            }
            netzwerkAuswahlAnzeigen = false;
            steuerung = new SpielSteuerung(this, ip);
            verbindungsLabel.setText("Verbinde mit Server...");
            versteckeAlleButtons();
            repaint();
            
            // Starte einen Timer, der den Verbindungsstatus überprüft
            Timer verbindungsTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (steuerung != null && steuerung.isVerbunden()) {
                        verbindungsLabel.setText("Verbunden mit Server");
                        zeigeSpielmodusAuswahl();
                        ((Timer)evt.getSource()).stop();
                    }
                }
            });
            verbindungsTimer.start();
        });

        // Spielmodus-Button ActionListener
        einfachButton.addActionListener(e -> startSpiel(SpielModus.EINFACH));
        mittelButton.addActionListener(e -> startSpiel(SpielModus.MITTEL));
        schwerButton.addActionListener(e -> startSpiel(SpielModus.SCHWER));

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
        add(infoLabel, gbc);
        gbc.gridy++;
        add(hostButton, gbc);
        gbc.gridy++;
        add(ipTextField, gbc);
        gbc.gridy++;
        add(clientButton, gbc);
        gbc.gridy++;
        add(verbindungsLabel, gbc);
    }

    /**
     * Zeigt die initiale Netzwerkauswahl an
     */
    private void zeigeNetzwerkAuswahl() {
        removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        add(titelLabel, gbc);
        gbc.gridy++;
        add(new JLabel("Wähle Spielmodus:"), gbc);
        gbc.gridy++;
        add(hostButton, gbc);
        gbc.gridy++;
        add(ipTextField, gbc);
        gbc.gridy++;
        add(clientButton, gbc);
        gbc.gridy++;
        add(verbindungsLabel, gbc);

        // Verstecke Spielmodus-Buttons
        einfachButton.setVisible(false);
        mittelButton.setVisible(false);
        schwerButton.setVisible(false);

        revalidate();
        repaint();
    }

    /**
     * Zeigt die Spielmodus-Auswahl an
     */
    private void zeigeSpielmodusAuswahl() {
        removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        add(titelLabel, gbc);
        gbc.gridy++;
        add(infoLabel, gbc);
        
        // Zeige Spielmodus-Buttons nur für den Host
        if (steuerung != null && steuerung.istHost()) {
            gbc.gridy++;
            add(einfachButton, gbc);
            gbc.gridy++;
            add(mittelButton, gbc);
            gbc.gridy++;
            add(schwerButton, gbc);
            
            einfachButton.setVisible(true);
            mittelButton.setVisible(true);
            schwerButton.setVisible(true);
        } else {
            // Für den Client: Zeige nur eine Meldung, dass auf den Host gewartet wird
            gbc.gridy++;
            infoLabel.setText("Warte auf Spielmodus-Auswahl des Hosts...");
        }
        
        gbc.gridy++;
        add(verbindungsLabel, gbc);

        revalidate();
        repaint();
    }

    /**
     * Versteckt alle Buttons und Labels
     */
    private void versteckeAlleButtons() {
        hostButton.setVisible(false);
        clientButton.setVisible(false);
        ipTextField.setVisible(false);
        einfachButton.setVisible(false);
        mittelButton.setVisible(false);
        schwerButton.setVisible(false);
        infoLabel.setVisible(false);
        titelLabel.setVisible(false);
        verbindungsLabel.setVisible(false);
    }

    /**
     * Markiert das Spiel als gestartet und versteckt alle UI-Elemente
     */
    public void spielGestartet() {
        spielGestartet = true;
        versteckeAlleButtons();
        addKeyListener(steuerung);
        requestFocusInWindow();
    }

    /**
     * Startet das Spiel im gewählten Modus
     */
    private void startSpiel(SpielModus modus) {
        spielGestartet = true;
        versteckeAlleButtons();
        steuerung.setModus(modus);
        addKeyListener(steuerung);
        requestFocusInWindow();
        
        // Starte den Spielthread
        spielThread = new Thread(steuerung);
        spielThread.start();
        
        // Markiere das Spiel als gestartet
        spielGestartet();
    }

    /**
     * Setzt das Spiel zurück
     */
    public void resetSpiel() {
        spielGestartet = false;
        if (steuerung != null) {
            steuerung.beendeSpiel();
        }
        removeKeyListener(steuerung);
        zeigeNetzwerkAuswahl();
    }

    /**
     * Zeichnet das Spielfeld.
     *
     * @param g Das Graphics-Objekt zum Zeichnen.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // Zweck: Hintergrund der Komponente vor dem Zeichnen gelöscht wird,
        // füllen des Hintergrunds mit der aktuellen usw.
        if (spielGestartet) {
            steuerung.zeichneSpielfeld(g); // Zeichnet das Spielfeld, wenn das Spiel gestartet ist
        }
    }

    /**
     * Zeigt eine Pause-Nachricht in einer Box an
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

        JLabel nachrichtLabel = new JLabel(nachricht);
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
}

