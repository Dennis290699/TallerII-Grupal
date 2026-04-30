package com.programacion;

import com.formdev.flatlaf.FlatDarkLaf;
import com.programacion.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Principal {
    public static void main(String[] args) {
        try {
            // Empezamos con el tema oscuro
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}