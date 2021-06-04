package processing.app.ui;

import javax.swing.*;

public class EditorHints extends JScrollPane {
    private final JPanel PANEL;

    public EditorHints() {
        PANEL = new JPanel();
        setViewportView(PANEL);
        PANEL.add(new JLabel("test"));
    }

}
