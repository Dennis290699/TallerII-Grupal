package com.programacion;

import com.formdev.flatlaf.FlatDarkLaf;
import com.programacion.ui.MainFrame;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;

public class Principal {
    public static void main(String[] args) {
        try {
            // Habilitar antialiasing de texto de alta calidad
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");

            // Configurar propiedades de diseño FlatLaf globales
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("CheckBox.arc", 6);
            UIManager.put("ProgressBar.horizontalSize", new Dimension(100, 8));
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 34);
            UIManager.put("TabbedPane.tabSelectionHeight", 3);
            UIManager.put("TitlePane.unifiedBackground", true);

            // Color de acento moderno (Indigo)
            UIManager.put("AccentColor", "#6366f1");
            UIManager.put("AccentColor2", "#4f46e5");

            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}