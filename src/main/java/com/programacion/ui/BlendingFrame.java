package com.programacion.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BlendingFrame extends JPanel {
    private MainFrame parentFrame;
    private boolean isDarkMode;

    private ImagePreviewPanel previewPanel = new ImagePreviewPanel();
    private JPanel placeholderPanel;
    private JScrollPane scrollMain;

    private DefaultListModel<BlendLayer> modeloCapas = new DefaultListModel<>();
    private JList<BlendLayer> listCapas = new JList<>(modeloCapas);

    private JButton btnVolver, btnGuardar, btnLimpiar, btnTema;
    private JButton btnAgregar, btnEliminar, btnSubir, btnBajar;

    private JPanel panelAjustes;
    private JLabel lblCapaActiva;
    private JComboBox<BlendMode> comboModo;
    private JSlider sliderOpacidad;

    private boolean updatingSelection = false;
    private Thread blendThread = null;

    public BlendingFrame(MainFrame parentFrame, boolean isDarkMode) {
        this.parentFrame = parentFrame;
        this.isDarkMode = isDarkMode;

        setLayout(new BorderLayout());

        initToolbar();
        initWorkspace();
        initRightSidebar();

        updateIcons();
        actualizarVistaLienzo();
    }

    private void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 10, 5, 10));

        btnVolver = new JButton("Volver al Editor Simple");
        btnVolver.addActionListener(e -> volverAlPrincipal());

        btnGuardar = new JButton("Guardar Resultado");
        btnGuardar.addActionListener(e -> accionGuardar());

        btnLimpiar = new JButton("Limpiar Todo");
        btnLimpiar.addActionListener(e -> accionLimpiarTodo());

        btnTema = new JButton("Tema");
        btnTema.addActionListener(e -> accionCambiarTema());

        toolBar.add(btnVolver);
        toolBar.addSeparator();
        toolBar.add(btnGuardar);
        toolBar.addSeparator();
        toolBar.add(btnLimpiar);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnTema);

        add(toolBar, BorderLayout.NORTH);
    }

    private void updateIcons() {
        btnGuardar.setIcon(loadIcon("/assets/icons/save.png", 20));
        btnTema.setIcon(loadIcon("/assets/icons/theme.png", 20));
        ImageIcon clearIcon = loadIcon("/assets/icons/delete.png", 20);
        if (clearIcon != null) btnLimpiar.setIcon(clearIcon);
    }

    private ImageIcon loadIcon(String path, int size) {
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) return null;
            BufferedImage img = ImageIO.read(imgUrl);
            if (!isDarkMode) img = invertImageColors(img);
            Image resized = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        } catch (Exception e) { return null; }
    }

    private BufferedImage invertImageColors(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = image.getRGB(x, y);
                Color c = new Color(p, true);
                res.setRGB(x, y, new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), c.getAlpha()).getRGB());
            }
        }
        return res;
    }

    private void initWorkspace() {
        // Inicializar el placeholder panel para cuando no hay imágenes
        placeholderPanel = new JPanel(new GridBagLayout());
        placeholderPanel.setBackground(UIManager.getColor("Panel.background"));

        JLabel lblPlaceholder = new JLabel(
            "<html><center><font size='12'>🥞</font><br><br>" +
            "<font size='5'><b>Blending Multicapa</b></font><br><br>" +
            "Añada dos o más imágenes en el panel derecho para comenzar a realizar mezclas.<br>" +
            "Las capas superiores se adaptarán a la resolución de la capa base (la más inferior).<br>" +
            "Puede ajustar el modo de mezcla y la opacidad individualmente por capa.</center></html>"
        );
        lblPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        lblPlaceholder.setForeground(UIManager.getColor("Label.disabledForeground"));
        placeholderPanel.add(lblPlaceholder);

        scrollMain = new JScrollPane(placeholderPanel);
        scrollMain.setBorder(BorderFactory.createEmptyBorder());

        add(scrollMain, BorderLayout.CENTER);
    }

    private void initRightSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Título de la sección de capas
        sidebar.add(crearEncabezado("CAPAS DE IMÁGENES", false));

        // Lista de capas
        listCapas.setCellRenderer(new LayerCellRenderer());
        listCapas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listCapas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarPanelAjustes();
            }
        });

        JScrollPane scrollCapas = new JScrollPane(listCapas);
        scrollCapas.setPreferredSize(new Dimension(250, 200));
        scrollCapas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        scrollCapas.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        sidebar.add(scrollCapas);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        // Botones de acción para las capas
        JPanel panelBotonesCapas = new JPanel(new GridLayout(2, 2, 5, 5));
        panelBotonesCapas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        btnAgregar = new JButton("Añadir...");
        btnAgregar.addActionListener(e -> accionAñadirCapa());
        btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> accionEliminarCapa());
        btnSubir = new JButton("Subir [↑]");
        btnSubir.addActionListener(e -> accionSubirCapa());
        btnBajar = new JButton("Bajar [↓]");
        btnBajar.addActionListener(e -> accionBajarCapa());

        panelBotonesCapas.add(btnAgregar);
        panelBotonesCapas.add(btnEliminar);
        panelBotonesCapas.add(btnSubir);
        panelBotonesCapas.add(btnBajar);
        sidebar.add(panelBotonesCapas);

        // Sección de ajustes de capa
        sidebar.add(crearEncabezado("AJUSTES DE CAPA", true));

        panelAjustes = new JPanel();
        panelAjustes.setLayout(new BoxLayout(panelAjustes, BoxLayout.Y_AXIS));
        panelAjustes.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblCapaActiva = new JLabel("Seleccione una capa");
        lblCapaActiva.setFont(lblCapaActiva.getFont().deriveFont(Font.BOLD, 12f));
        lblCapaActiva.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelAjustes.add(lblCapaActiva);
        panelAjustes.add(Box.createRigidArea(new Dimension(0, 10)));

        // Dropdown de Modo de Blend
        JPanel panelCombo = new JPanel(new BorderLayout());
        panelCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelCombo.setBorder(BorderFactory.createTitledBorder("Modo de Mezcla"));
        comboModo = new JComboBox<>(BlendMode.values());
        comboModo.addActionListener(e -> {
            if (updatingSelection) return;
            int idx = listCapas.getSelectedIndex();
            if (idx >= 0) {
                BlendLayer layer = modeloCapas.getElementAt(idx);
                layer.setBlendMode((BlendMode) comboModo.getSelectedItem());
                listCapas.repaint();
                triggerBlending();
            }
        });
        panelCombo.add(comboModo, BorderLayout.CENTER);
        panelCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        panelAjustes.add(panelCombo);
        panelAjustes.add(Box.createRigidArea(new Dimension(0, 10)));

        // Slider de Opacidad
        JPanel panelSlider = new JPanel(new BorderLayout());
        panelSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelSlider.setBorder(BorderFactory.createTitledBorder("Opacidad (Transparencia)"));
        sliderOpacidad = new JSlider(0, 100, 50);
        sliderOpacidad.setMajorTickSpacing(25);
        sliderOpacidad.setPaintTicks(true);
        sliderOpacidad.setPaintLabels(true);
        sliderOpacidad.addChangeListener(e -> {
            if (updatingSelection) return;
            int idx = listCapas.getSelectedIndex();
            if (idx >= 0) {
                BlendLayer layer = modeloCapas.getElementAt(idx);
                layer.setOpacity(sliderOpacidad.getValue() / 100.0f);
                listCapas.repaint();
                triggerBlending();
            }
        });
        panelSlider.add(sliderOpacidad, BorderLayout.CENTER);
        panelSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        panelAjustes.add(panelSlider);

        panelAjustes.setVisible(false); // Oculto hasta seleccionar una capa
        sidebar.add(panelAjustes);

        sidebar.add(Box.createVerticalGlue());

        JScrollPane scrollSidebar = new JScrollPane(sidebar);
        scrollSidebar.setPreferredSize(new Dimension(280, 0));
        scrollSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor")));
        add(scrollSidebar, BorderLayout.EAST);
    }

    private JPanel crearEncabezado(String titulo, boolean conMargenTop) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(lbl, BorderLayout.WEST);
        panel.add(new JSeparator(), BorderLayout.SOUTH);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        int top = conMargenTop ? 20 : 0;
        panel.setBorder(BorderFactory.createEmptyBorder(top, 0, 8, 0));
        return panel;
    }

    private void actualizarVistaLienzo() {
        if (modeloCapas.isEmpty()) {
            scrollMain.setViewportView(placeholderPanel);
            btnGuardar.setEnabled(false);
            btnLimpiar.setEnabled(false);
        } else {
            scrollMain.setViewportView(previewPanel);
            btnGuardar.setEnabled(true);
            btnLimpiar.setEnabled(true);
        }
        revalidate();
        repaint();
    }

    private void actualizarPanelAjustes() {
        int idx = listCapas.getSelectedIndex();
        if (idx >= 0) {
            BlendLayer layer = modeloCapas.getElementAt(idx);
            updatingSelection = true;
            
            lblCapaActiva.setText("Capa: " + getTruncatedName(layer.getName(), 22));
            lblCapaActiva.setToolTipText(layer.getName());
            comboModo.setSelectedItem(layer.getBlendMode());
            sliderOpacidad.setValue((int) (layer.getOpacity() * 100));

            // Si es la capa base (el elemento de más abajo en la pila, o sea, el último del modelo),
            // la opacidad y modo de blend no afectan porque no tiene nada debajo. Le mostramos un aviso sutil.
            boolean esBase = (idx == modeloCapas.getSize() - 1);
            comboModo.setEnabled(!esBase);
            sliderOpacidad.setEnabled(!esBase);
            
            updatingSelection = false;
            panelAjustes.setVisible(true);
        } else {
            panelAjustes.setVisible(false);
        }
        panelAjustes.revalidate();
        panelAjustes.repaint();
    }

    private void accionAñadirCapa() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "png", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(() -> {
                List<BlendLayer> nuevasCapas = new ArrayList<>();
                for (File file : files) {
                    try {
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            nuevasCapas.add(new BlendLayer(img, file));
                        }
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error al cargar: " + file.getName()));
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    // Insertamos las nuevas capas al inicio de la lista (arriba en la pila)
                    for (BlendLayer capa : nuevasCapas) {
                        modeloCapas.add(0, capa);
                    }
                    invalidateAllLayerCaches();
                    actualizarVistaLienzo();
                    if (!modeloCapas.isEmpty()) {
                        listCapas.setSelectedIndex(0);
                    }
                    setCursor(Cursor.getDefaultCursor());
                    triggerBlending();
                });
            }).start();
        }
    }

    private void accionEliminarCapa() {
        int idx = listCapas.getSelectedIndex();
        if (idx >= 0) {
            modeloCapas.remove(idx);
            invalidateAllLayerCaches();
            actualizarVistaLienzo();
            
            // Re-seleccionar un elemento vecino
            int size = modeloCapas.size();
            if (size > 0) {
                int nextIdx = Math.min(idx, size - 1);
                listCapas.setSelectedIndex(nextIdx);
            }
            triggerBlending();
        }
    }

    private void accionSubirCapa() {
        int idx = listCapas.getSelectedIndex();
        // Subir visualmente significa bajar el índice (ir hacia el index 0)
        if (idx > 0) {
            BlendLayer selected = modeloCapas.remove(idx);
            modeloCapas.add(idx - 1, selected);
            invalidateAllLayerCaches();
            listCapas.setSelectedIndex(idx - 1);
            triggerBlending();
        }
    }

    private void accionBajarCapa() {
        int idx = listCapas.getSelectedIndex();
        // Bajar visualmente significa subir el índice (ir hacia el index size-1)
        if (idx >= 0 && idx < modeloCapas.getSize() - 1) {
            BlendLayer selected = modeloCapas.remove(idx);
            modeloCapas.add(idx + 1, selected);
            invalidateAllLayerCaches();
            listCapas.setSelectedIndex(idx + 1);
            triggerBlending();
        }
    }

    private void invalidateAllLayerCaches() {
        for (int i = 0; i < modeloCapas.size(); i++) {
            modeloCapas.getElementAt(i).invalidateCache();
        }
    }

    private synchronized void triggerBlending() {
        if (blendThread != null && blendThread.isAlive()) {
            blendThread.interrupt();
        }

        int size = modeloCapas.getSize();
        if (size == 0) {
            SwingUtilities.invokeLater(() -> {
                previewPanel.setImage(null);
                actualizarVistaLienzo();
            });
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        blendThread = new Thread(() -> {
            try {
                BufferedImage result = performBlending();
                if (!Thread.currentThread().isInterrupted()) {
                    SwingUtilities.invokeLater(() -> {
                        previewPanel.setImage(result);
                        actualizarVistaLienzo();
                        setCursor(Cursor.getDefaultCursor());
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        });
        blendThread.start();
    }

    private BufferedImage performBlending() {
        int size = modeloCapas.getSize();
        if (size == 0) return null;
        if (size == 1) {
            return modeloCapas.getElementAt(0).getOriginalImage();
        }

        // La última capa de la lista es la capa Base (Fondo)
        BlendLayer baseLayer = modeloCapas.getElementAt(size - 1);
        BufferedImage baseImg = baseLayer.getOriginalImage();
        int w = baseImg.getWidth();
        int h = baseImg.getHeight();

        BufferedImage blended = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = blended.getGraphics();
        graphics.drawImage(baseImg, 0, 0, null);
        graphics.dispose();

        // Procesar las capas superiores de abajo hacia arriba (desde size - 2 hasta 0)
        for (int i = size - 2; i >= 0; i--) {
            if (Thread.currentThread().isInterrupted()) return null;
            BlendLayer layer = modeloCapas.getElementAt(i);
            BufferedImage layerImg = layer.getScaledImage(w, h);

            float opacity = layer.getOpacity();
            BlendMode mode = layer.getBlendMode();

            // Cargar arreglos de píxeles
            int[] pixels1 = new int[w * h];
            blended.getRGB(0, 0, w, h, pixels1, 0, w);

            int[] pixels2 = new int[w * h];
            layerImg.getRGB(0, 0, w, h, pixels2, 0, w);

            for (int idx = 0; idx < pixels1.length; idx++) {
                if (idx % 50000 == 0 && Thread.currentThread().isInterrupted()) return null;

                int p1 = pixels1[idx];
                int p2 = pixels2[idx];

                int r1 = (p1 >> 16) & 0xFF;
                int g1 = (p1 >> 8) & 0xFF;
                int b1 = p1 & 0xFF;

                int r2 = (p2 >> 16) & 0xFF;
                int g2 = (p2 >> 8) & 0xFF;
                int b2 = p2 & 0xFF;

                int r_blend = r2, g_blend = g2, b_blend = b2;

                if (mode == BlendMode.SUMATIVE) {
                    r_blend = Math.min(255, r1 + r2);
                    g_blend = Math.min(255, g1 + g2);
                    b_blend = Math.min(255, b1 + b2);
                } else if (mode == BlendMode.MULTIPLICATIVE) {
                    r_blend = (r1 * r2) / 255;
                    g_blend = (g1 * g2) / 255;
                    b_blend = (b1 * b2) / 255;
                }

                // Interpolación basada en opacidad de la capa
                int r = (int) ((1.0f - opacity) * r1 + opacity * r_blend);
                int g = (int) ((1.0f - opacity) * g1 + opacity * g_blend);
                int b = (int) ((1.0f - opacity) * b1 + opacity * b_blend);

                pixels1[idx] = (r << 16) | (g << 8) | b;
            }
            blended.setRGB(0, 0, w, h, pixels1, 0, w);
        }

        return blended;
    }

    private void accionGuardar() {
        BufferedImage img = previewPanel.getImage();
        if (img == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Imagen", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File output = chooser.getSelectedFile();
                if (!output.getName().toLowerCase().endsWith(".png")) {
                    output = new File(output.getAbsolutePath() + ".png");
                }
                ImageIO.write(img, "png", output);
                JOptionPane.showMessageDialog(this, "Imagen guardada exitosamente!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar la imagen.");
            }
        }
    }

    private void accionLimpiarTodo() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar todas las capas?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            modeloCapas.clear();
            actualizarVistaLienzo();
            actualizarPanelAjustes();
            triggerBlending();
        }
    }

    private void accionCambiarTema() {
        try {
            isDarkMode = !isDarkMode;
            updateIcons();
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(parentFrame);
            parentFrame.syncTheme(isDarkMode); // Sincronizar tema con la ventana padre
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void syncTheme(boolean isDark) {
        if (this.isDarkMode != isDark) {
            this.isDarkMode = isDark;
            updateIcons();
            try {
                if (isDarkMode) UIManager.setLookAndFeel(new FlatDarkLaf());
                else UIManager.setLookAndFeel(new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(parentFrame);
            } catch (Exception e) {}
        }
    }

    private void volverAlPrincipal() {
        if (blendThread != null && blendThread.isAlive()) {
            blendThread.interrupt();
        }
        parentFrame.mostrarSimpleMode();
    }

    private String getTruncatedName(String fullName, int maxLength) {
        if (fullName == null) return "";
        if (fullName.length() <= maxLength) return fullName;

        int dotIdx = fullName.lastIndexOf('.');
        if (dotIdx > 0 && fullName.length() - dotIdx <= 6) {
            String baseName = fullName.substring(0, dotIdx);
            String extension = fullName.substring(dotIdx);
            int baseMaxLength = maxLength - extension.length() - 3;
            if (baseMaxLength > 0) {
                return baseName.substring(0, baseMaxLength) + "..." + extension;
            }
        }
        return fullName.substring(0, maxLength - 3) + "...";
    }

    // --- ENUMS & CLASES AUXILIARES ---

    public enum BlendMode {
        ALPHA("Alpha Blending"),
        SUMATIVE("Sumativo"),
        MULTIPLICATIVE("Multiplicativo");

        private final String displayName;
        BlendMode(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public static class BlendLayer {
        private BufferedImage originalImage;
        private BufferedImage scaledImage;
        private BufferedImage thumbnail;
        private File file;
        private String name;
        private BlendMode blendMode = BlendMode.ALPHA;
        private float opacity = 0.5f;

        public BlendLayer(BufferedImage img, File file) {
            this.originalImage = img;
            this.file = file;
            this.name = file != null ? file.getName() : "Imagen Sin Nombre";

            // Crear miniatura para la JList
            this.thumbnail = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = this.thumbnail.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, 40, 40, null);
            g.dispose();
        }

        public BufferedImage getOriginalImage() { return originalImage; }
        public BufferedImage getThumbnail() { return thumbnail; }
        public String getName() { return name; }
        public BlendMode getBlendMode() { return blendMode; }
        public void setBlendMode(BlendMode blendMode) { this.blendMode = blendMode; }
        public float getOpacity() { return opacity; }
        public void setOpacity(float opacity) { this.opacity = opacity; }

        public BufferedImage getScaledImage(int w, int h) {
            if (scaledImage != null && scaledImage.getWidth() == w && scaledImage.getHeight() == h) {
                return scaledImage;
            }
            scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, w, h, null);
            g2d.dispose();
            return scaledImage;
        }

        public void invalidateCache() {
            scaledImage = null;
        }
    }

    private class LayerCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            BlendLayer layer = (BlendLayer) value;

            // Miniatura
            JLabel lblIcon = new JLabel();
            lblIcon.setPreferredSize(new Dimension(40, 40));
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
            lblIcon.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
            if (layer.getThumbnail() != null) {
                lblIcon.setIcon(new ImageIcon(layer.getThumbnail()));
            }

            // Etiquetas de texto
            JPanel textPanel = new JPanel(new GridLayout(2, 1, 2, 2));
            textPanel.setOpaque(false);

            String displayName = getTruncatedName(layer.getName(), 20);
            JLabel lblName = new JLabel(displayName);
            lblName.setFont(lblName.getFont().deriveFont(Font.BOLD, 12f));
            panel.setToolTipText(layer.getName());

            String info = String.format("Modo: %s | Opacidad: %d%%", layer.getBlendMode().toString(), (int)(layer.getOpacity() * 100));
            if (index == list.getModel().getSize() - 1) {
                info += " (Base / Fondo)";
            }
            JLabel lblInfo = new JLabel(info);
            lblInfo.setFont(lblInfo.getFont().deriveFont(10f));

            textPanel.add(lblName);
            textPanel.add(lblInfo);

            panel.add(lblIcon, BorderLayout.WEST);
            panel.add(textPanel, BorderLayout.CENTER);

            // Ajuste de colores según selección
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                lblName.setForeground(list.getSelectionForeground());
                lblInfo.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                lblName.setForeground(list.getForeground());
                lblInfo.setForeground(UIManager.getColor("Label.disabledForeground"));
            }

            return panel;
        }
    }
}
