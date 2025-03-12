import javax.swing.*;

/**
 * Die Klasse SpielRahmen ist ein JFrame, das das Hauptfenster des Pong-Spiels darstellt.
 */
public class SpielRahmen extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startGui();
            }
        });
    }

    private static void startGui() {
        JFrame f = new SpielRahmen();
        f.setTitle("Pong");
        f.setSize(800, 600);
        f.setResizable(false); // Größe des Fensters ist nicht veränderbar
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null); // Setzt die Position des Fensters in der Mitte des Bildschirms
        f.add(new SpielFeld()); // Fügt das Spielfeld zum Fenster hinzu
        f.setVisible(true); // um das Fenster sichtbar zu machen
    }
} 