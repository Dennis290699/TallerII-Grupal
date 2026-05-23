package com.programacion.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.programacion.core.ImageFilter;
import com.programacion.filters.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Stack;

public class AdvancedEditorFrame extends JPanel {
    private MainFrame parentFrame;
    private BufferedImage originalImage;
    private BufferedImage currentImage;
    private BufferedImage previewImage;
    private Stack<BufferedImage> history = new Stack<>();
    private Stack<String> filterNames = new Stack<>();

    private ImagePreviewPanel previewPanel = new ImagePreviewPanel();
    private JScrollPane scrollMain;
    private JLayeredPane layeredPane;
    private HistogramPanel miniHistogramPanel;
    private boolean isDarkMode;

    private JButton btnGuardar, btnDeshacer, btnReiniciar, btnVolver, btnTema;

    // Visual history
    private DefaultListModel<String> modelHistorial = new DefaultListModel<>();
    private JList<String> listHistorial;

    // Ventanas flotantes
    private JDialog ventanaTransparencia = null;
    private JDialog ventanaMascara = null;
    private JDialog ventanaRGB = null;
    private JDialog ventanaHSV = null;
    private JDialog ventanaMatrices = null;
    private JDialog ventanaHistograma = null;
    private Thread previewThread = null;

    private int ultimoValorTransparencia = 100;
    private int ultimoValorMascara = 8;
    private int valR = 0, valG = 0, valB = 0;
    private int valH = 0, valS = 0, valV = 0;

    private float[] ultimoValorMatriz = ColorMatrixFilter.getNeutral().clone();
    private int ultimoPresetMatriz = 0;

    private String activeFilterName = null;
    private boolean modificandoUltimoPaso = false;
    private BufferedImage tempOriginalImageForEdit = null;

    private int backupValorTransparencia;
    private int backupValorMascara;
    private int backupValR, backupValG, backupValB;
    private int backupValH, backupValS, backupValV;
    private float[] backupMatrix = new float[20];
    private int backupPresetMatriz;

    public AdvancedEditorFrame(MainFrame parentFrame, BufferedImage initialImage, boolean isDarkMode) {
        this.parentFrame = parentFrame;
        this.originalImage = copyImage(initialImage);
        this.currentImage = copyImage(initialImage);
        this.isDarkMode = isDarkMode;

        setLayout(new BorderLayout());

        initToolbar();
        initWorkspace();
        initRightSidebar();

        updateIcons();
        mostrarImagen(currentImage);
        actualizarHistorialVista();
    }

    private void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(6, 12, 6, 12));

        // Branding de LuminaFX
        JLabel lblLogo = new JLabel("✨ LuminaFX | Modo Apilado");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblLogo.setForeground(new Color(99, 102, 241));
        toolBar.add(lblLogo);
        toolBar.add(Box.createRigidArea(new Dimension(15, 0)));

        btnVolver = new JButton("Volver");
        btnVolver.putClientProperty("JButton.buttonType", "toolBarButton");
        btnVolver.addActionListener(e -> volverAlPrincipal());

        btnGuardar = new JButton("Guardar");
        btnGuardar.putClientProperty("JButton.buttonType", "toolBarButton");
        btnGuardar.addActionListener(e -> accionGuardar());

        btnDeshacer = new JButton("Deshacer");
        btnDeshacer.putClientProperty("JButton.buttonType", "toolBarButton");
        btnDeshacer.addActionListener(e -> accionDeshacer());

        btnReiniciar = new JButton("Reiniciar Todo");
        btnReiniciar.putClientProperty("JButton.buttonType", "toolBarButton");
        btnReiniciar.addActionListener(e -> accionReiniciar());

        btnTema = new JButton();
        btnTema.putClientProperty("JButton.buttonType", "toolBarButton");
        btnTema.addActionListener(e -> accionCambiarTema());

        toolBar.add(btnVolver);
        toolBar.addSeparator();
        toolBar.add(btnGuardar);
        toolBar.addSeparator();
        toolBar.add(btnDeshacer);
        toolBar.addSeparator();
        toolBar.add(btnReiniciar);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnTema);

        add(toolBar, BorderLayout.NORTH);
    }

    private void updateIcons() {
        btnVolver.setIcon(new ModernIcon("volver", 16));
        btnGuardar.setIcon(new ModernIcon("guardar", 16));
        btnDeshacer.setIcon(new ModernIcon("deshacer", 16));
        btnReiniciar.setIcon(new ModernIcon("reiniciar", 16));
        btnTema.setIcon(new ModernIcon("tema", 16));
    }

    private ImageIcon loadIcon(String path, int size) {
        return null;
    }

    private BufferedImage invertImageColors(BufferedImage image) {
        return null;
    }

    private void initWorkspace() {
        layeredPane = new JLayeredPane();

        scrollMain = new JScrollPane(previewPanel);
        scrollMain.setBorder(BorderFactory.createEmptyBorder());

        miniHistogramPanel = new HistogramPanel();
        miniHistogramPanel.setOverlay(true);
        miniHistogramPanel.setMode(HistogramPanel.Mode.RGB);
        miniHistogramPanel.setVisible(false);

        // Mantener para ver Original (funcionalidad extra en el panel)
        previewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (originalImage != null && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                    previewPanel.setImage(originalImage);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentImage != null && e.getButton() == MouseEvent.BUTTON1) {
                    previewPanel.setImage(currentImage);
                }
            }
        });

        layeredPane.add(scrollMain, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(miniHistogramPanel, JLayeredPane.PALETTE_LAYER);

        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                scrollMain.setBounds(0, 0, w, h);
                int hw = 220;
                int hh = 150;
                miniHistogramPanel.setBounds(w - hw - 20, h - hh - 20, hw, hh);
            }
        });

        add(layeredPane, BorderLayout.CENTER);
    }

    private void initRightSidebar() {
        JPanel sidebarWrapper = new JPanel(new BorderLayout());
        sidebarWrapper.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor")));
        sidebarWrapper.setPreferredSize(new Dimension(300, 0));

        JTabbedPane tabbedPane = new JTabbedPane();

        // Pestaña 1: Filtros de Color y Efectos
        JPanel tabFiltros = new JPanel();
        tabFiltros.setLayout(new BoxLayout(tabFiltros, BoxLayout.Y_AXIS));
        tabFiltros.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tabFiltros.add(crearEncabezado("FILTROS DE COLOR", false));
        agregarBotonFiltro(tabFiltros, new GrayscaleFilter(255));
        agregarBotonFiltro(tabFiltros, new NegativeFilter());
        agregarBotonFiltro(tabFiltros, new BlackAndWhiteFilter());

        tabFiltros.add(crearEncabezado("EFECTOS VISUALES", true));
        agregarBotonFiltro(tabFiltros, new FrostedGlassFilter());
        agregarBotonFiltro(tabFiltros, new CircularFadeFilter());

        tabFiltros.add(crearEncabezado("ENFOQUE Y DESENFOQUE", true));
        ImageFilter[] convoluciones = {
            ConvolutionFilter.Enfoque(), ConvolutionFilter.Desenfoque(), ConvolutionFilter.DesenfoquePesado(),
            ConvolutionFilter.Bordes(), ConvolutionFilter.Aclarar(), ConvolutionFilter.Oscurecer()
        };
        JComboBox<String> comboConvolucion = new JComboBox<>();
        comboConvolucion.addItem("Seleccionar...");
        for (ImageFilter f : convoluciones) comboConvolucion.addItem(f.getName());
        comboConvolucion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        comboConvolucion.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboConvolucion.addActionListener(e -> {
            int idx = comboConvolucion.getSelectedIndex();
            if (idx > 0) {
                cerrarVentanasFlotantes();
                aplicarFiltro(convoluciones[idx - 1]);
                comboConvolucion.setSelectedIndex(0);
            }
        });
        tabFiltros.add(comboConvolucion);
        tabFiltros.add(Box.createVerticalGlue());

        JScrollPane scrollFiltros = new JScrollPane(tabFiltros);
        scrollFiltros.setBorder(null);
        tabbedPane.addTab("Filtros", new ModernIcon("avanzado", 14), scrollFiltros);

        // Pestaña 2: Ajustes Dinámicos
        JPanel tabAjustes = new JPanel();
        tabAjustes.setLayout(new BoxLayout(tabAjustes, BoxLayout.Y_AXIS));
        tabAjustes.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tabAjustes.add(crearEncabezado("HERRAMIENTAS DINÁMICAS", false));

        JButton btnTrans = new JButton("Transparencia Ajustable");
        btnTrans.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnTrans.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTrans.putClientProperty("JButton.buttonType", "roundRect");
        btnTrans.addActionListener(e -> accionTransparenciaAjustable());
        tabAjustes.add(btnTrans);
        tabAjustes.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnMasc = new JButton("Máscaras de Bits");
        btnMasc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnMasc.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMasc.putClientProperty("JButton.buttonType", "roundRect");
        btnMasc.addActionListener(e -> accionMascaraBitsAdjustable());
        tabAjustes.add(btnMasc);
        tabAjustes.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnRGB = new JButton("Tonalidades RGB");
        btnRGB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnRGB.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRGB.putClientProperty("JButton.buttonType", "roundRect");
        btnRGB.setBackground(new Color(99, 102, 241));
        btnRGB.setForeground(Color.WHITE);
        btnRGB.addActionListener(e -> accionAjusteRGB());
        tabAjustes.add(btnRGB);
        tabAjustes.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnHSV = new JButton("Ajuste HSV");
        btnHSV.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnHSV.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHSV.putClientProperty("JButton.buttonType", "roundRect");
        btnHSV.addActionListener(e -> accionAjusteHSV());
        tabAjustes.add(btnHSV);
        tabAjustes.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnMatrices = new JButton("Matrices de Color");
        btnMatrices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnMatrices.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMatrices.putClientProperty("JButton.buttonType", "roundRect");
        btnMatrices.addActionListener(e -> accionMatricesColor());
        tabAjustes.add(btnMatrices);
        tabAjustes.add(Box.createVerticalGlue());

        JScrollPane scrollAjustes = new JScrollPane(tabAjustes);
        scrollAjustes.setBorder(null);
        tabbedPane.addTab("Ajustes", new ModernIcon("blending", 14), scrollAjustes);

        // Pestaña 3: Análisis
        JPanel tabAnalisis = new JPanel();
        tabAnalisis.setLayout(new BoxLayout(tabAnalisis, BoxLayout.Y_AXIS));
        tabAnalisis.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        tabAnalisis.add(crearEncabezado("ANÁLISIS DE IMAGEN", false));

        JButton btnHistograma = new JButton("Ver Histograma");
        btnHistograma.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnHistograma.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHistograma.putClientProperty("JButton.buttonType", "roundRect");
        btnHistograma.setBackground(new Color(99, 102, 241));
        btnHistograma.setForeground(Color.WHITE);
        btnHistograma.setIcon(new ModernIcon("histograma", 16, Color.WHITE));
        btnHistograma.addActionListener(e -> accionHistograma());
        tabAnalisis.add(btnHistograma);
        tabAnalisis.add(Box.createVerticalGlue());

        JScrollPane scrollAnalisis = new JScrollPane(tabAnalisis);
        scrollAnalisis.setBorder(null);
        tabbedPane.addTab("Análisis", new ModernIcon("histograma", 14), scrollAnalisis);

        sidebarWrapper.add(tabbedPane, BorderLayout.CENTER);

        // Panel inferior fijo para el HISTORIAL DE EFECTOS
        JPanel panelBottom = new JPanel(new BorderLayout());
        panelBottom.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));

        JPanel encabezadoHistorial = crearEncabezado("HISTORIAL DE EFECTOS", false);
        panelBottom.add(encabezadoHistorial, BorderLayout.NORTH);

        listHistorial = new JList<>(modelHistorial);
        listHistorial.setEnabled(false); // Solo visual
        listHistorial.setBackground(UIManager.getColor("Panel.background"));

        listHistorial.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                if (index > 0) {
                    label.setFont(label.getFont().deriveFont(Font.ITALIC, 11f));
                } else {
                    label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
                }
                return label;
            }
        });

        JScrollPane scrollHistorial = new JScrollPane(listHistorial);
        scrollHistorial.setPreferredSize(new Dimension(0, 140));
        scrollHistorial.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        panelBottom.add(scrollHistorial, BorderLayout.CENTER);

        sidebarWrapper.add(panelBottom, BorderLayout.SOUTH);
        add(sidebarWrapper, BorderLayout.EAST);
    }

    private JPanel crearEncabezado(String titulo, boolean conMargenTop) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(lbl, BorderLayout.WEST);
        panel.add(new JSeparator(), BorderLayout.SOUTH);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        int top = conMargenTop ? 18 : 0;
        panel.setBorder(BorderFactory.createEmptyBorder(top, 0, 8, 0));
        return panel;
    }

    private void agregarBotonFiltro(JPanel contenedor, ImageFilter filtro) {
        JButton btn = new JButton(filtro.getName());
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.addActionListener(e -> {
            cerrarVentanasFlotantes();
            aplicarFiltro(filtro);
        });
        contenedor.add(btn);
        contenedor.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private ImageIcon prepararImagenParaLabel(BufferedImage img) {
        if (img == null) return null;
        int targetWidth = scrollMain.getWidth() - 20;
        if (targetWidth <= 0) targetWidth = 800;
        double ratio = (double) img.getHeight() / img.getWidth();
        int targetHeight = (int) (targetWidth * ratio);
        Image escalada = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(escalada);
    }

    private void mostrarImagen(BufferedImage img) {
        if (img != null) {
            previewPanel.setImage(img);
            scrollMain.setViewportView(previewPanel);

            if (miniHistogramPanel != null) {
                miniHistogramPanel.setImage(img);
                miniHistogramPanel.setVisible(true);
            }

            if (ventanaHistograma != null && ventanaHistograma.isVisible()) {
                Component[] comps = ventanaHistograma.getContentPane().getComponents();
                for (Component c : comps) {
                    if (c instanceof HistogramPanel) {
                        ((HistogramPanel) c).setImage(img);
                        break;
                    }
                }
            }
        }
    }

    private void aplicarFiltro(ImageFilter filtro) {
        if (currentImage == null) return;

        // Guardamos el estado actual en el historial antes de aplicar el filtro
        history.push(copyImage(currentImage));
        filterNames.push(filtro.getName());

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            BufferedImage result = filtro.apply(currentImage);
            SwingUtilities.invokeLater(() -> {
                currentImage = result;
                mostrarImagen(currentImage);
                btnDeshacer.setEnabled(true);
                actualizarHistorialVista();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private synchronized void aplicarPrevisualizacionDinamica(ImageFilter filtro) {
        if (currentImage == null) return;
        if (previewThread != null && previewThread.isAlive()) {
            previewThread.interrupt();
        }

        previewThread = new Thread(() -> {
            BufferedImage result = filtro.apply(currentImage);
            if (!Thread.currentThread().isInterrupted()) {
                SwingUtilities.invokeLater(() -> {
                    previewImage = result;
                    mostrarImagen(previewImage);
                });
            }
        });
        previewThread.start();
    }

    private void prepararEdicionDinamica(String nombreFiltro) {
        if (!filterNames.isEmpty() && filterNames.peek().equals(nombreFiltro)) {
            modificandoUltimoPaso = true;
            tempOriginalImageForEdit = copyImage(currentImage);
            currentImage = history.pop();
            filterNames.pop();
            
            backupValorTransparencia = ultimoValorTransparencia;
            backupValorMascara = ultimoValorMascara;
            backupValR = valR; backupValG = valG; backupValB = valB;
            backupValH = valH; backupValS = valS; backupValV = valV;
            backupMatrix = ultimoValorMatriz.clone();
            backupPresetMatriz = ultimoPresetMatriz;
        } else {
            modificandoUltimoPaso = false;
            if (nombreFiltro.equals("Transparencia Ajustable")) {
                ultimoValorTransparencia = 100;
            } else if (nombreFiltro.equals("Máscaras de Bits")) {
                ultimoValorMascara = 8;
            } else if (nombreFiltro.equals("Ajuste Tonalidad RGB")) {
                valR = 0; valG = 0; valB = 0;
            } else if (nombreFiltro.equals("Ajuste HSV")) {
                valH = 0; valS = 0; valV = 0;
            } else if (nombreFiltro.equals("Matriz de Color")) {
                ultimoValorMatriz = ColorMatrixFilter.getNeutral().clone();
                ultimoPresetMatriz = 0;
            }
        }
    }

    private void confirmarHerramientaDinamica(String nombreFiltro) {
        if (previewImage != null) {
            history.push(copyImage(currentImage));
            filterNames.push(nombreFiltro);
            currentImage = copyImage(previewImage);
            previewImage = null;
            mostrarImagen(currentImage);
            actualizarHistorialVista();
        } else if (modificandoUltimoPaso) {
            history.push(copyImage(currentImage));
            filterNames.push(nombreFiltro);
            currentImage = tempOriginalImageForEdit;
            mostrarImagen(currentImage);
            actualizarHistorialVista();
        }
        modificandoUltimoPaso = false;
        cerrarVentanasFlotantes();
    }

    private void descartarHerramientaDinamica() {
        if (modificandoUltimoPaso) {
            if (activeFilterName != null) {
                if (activeFilterName.equals("Transparencia Ajustable")) {
                    ultimoValorTransparencia = backupValorTransparencia;
                } else if (activeFilterName.equals("Máscaras de Bits")) {
                    ultimoValorMascara = backupValorMascara;
                } else if (activeFilterName.equals("Ajuste Tonalidad RGB")) {
                    valR = backupValR; valG = backupValG; valB = backupValB;
                } else if (activeFilterName.equals("Ajuste HSV")) {
                    valH = backupValH; valS = backupValS; valV = backupValV;
                } else if (activeFilterName.equals("Matriz de Color")) {
                    ultimoValorMatriz = backupMatrix;
                    ultimoPresetMatriz = backupPresetMatriz;
                }
                history.push(copyImage(currentImage));
                filterNames.push(activeFilterName);
            }
            currentImage = tempOriginalImageForEdit;
        }
        previewImage = null;
        mostrarImagen(currentImage);
        modificandoUltimoPaso = false;
        cerrarVentanasFlotantes();
    }

    private void accionDeshacer() {
        if (!history.isEmpty()) {
            cerrarVentanasFlotantes();
            currentImage = history.pop();
            filterNames.pop();
            mostrarImagen(currentImage);
            actualizarHistorialVista();
        } else {
            JOptionPane.showMessageDialog(this, "No hay más acciones para deshacer.");
        }
    }

    private void accionReiniciar() {
        cerrarVentanasFlotantes();
        history.clear();
        filterNames.clear();
        currentImage = copyImage(originalImage);
        mostrarImagen(currentImage);
        actualizarHistorialVista();
    }

    private void actualizarHistorialVista() {
        modelHistorial.clear();
        modelHistorial.addElement("Original");
        for (int i = 0; i < filterNames.size(); i++) {
            modelHistorial.addElement(" + " + filterNames.get(i));
        }
        // Hacer autoscroll hasta el último elemento añadido
        int lastIndex = listHistorial.getModel().getSize() - 1;
        if (lastIndex >= 0) {
            listHistorial.ensureIndexIsVisible(lastIndex);
        }
    }

    private void accionGuardar() {
        if (currentImage == null) return;
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File output = chooser.getSelectedFile();
                if (!output.getName().toLowerCase().endsWith(".png")) {
                    output = new File(output.getAbsolutePath() + ".png");
                }
                ImageIO.write(currentImage, "png", output);
                JOptionPane.showMessageDialog(this, "Imagen guardada exitosamente!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar.");
            }
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
            parentFrame.syncTheme(isDarkMode); // Synchronize with parent
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void volverAlPrincipal() {
        cerrarVentanasFlotantes();
        parentFrame.mostrarSimpleMode();
    }

    // --- Herramientas Dinámicas ---
    private void accionTransparenciaAjustable() {
        if (ventanaTransparencia != null && ventanaTransparencia.isVisible()) {
            ventanaTransparencia.toFront(); return;
        }

        activeFilterName = "Transparencia Ajustable";
        prepararEdicionDinamica(activeFilterName);

        ventanaTransparencia = new JDialog(parentFrame, "Ajustar Transparencia", false);
        ventanaTransparencia.setLayout(new BorderLayout());

        JSlider slider = new JSlider(0, 100, ultimoValorTransparencia);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        slider.addChangeListener(e -> {
            ultimoValorTransparencia = slider.getValue();
            float factor = ultimoValorTransparencia / 100.0f;
            aplicarPrevisualizacionDinamica(new TransparenciaFilter(factor));
        });

        aplicarPrevisualizacionDinamica(new TransparenciaFilter(ultimoValorTransparencia / 100.0f));

        JPanel panelBotones = crearBotonesDinamicos(activeFilterName);
        ventanaTransparencia.add(slider, BorderLayout.CENTER);
        ventanaTransparencia.add(panelBotones, BorderLayout.SOUTH);
        ventanaTransparencia.pack();
        posicionarVentana(ventanaTransparencia, 70);
    }

    private void accionMascaraBitsAdjustable() {
        if (ventanaMascara != null && ventanaMascara.isVisible()) {
            ventanaMascara.toFront(); return;
        }

        activeFilterName = "Máscaras de Bits";
        prepararEdicionDinamica(activeFilterName);

        ventanaMascara = new JDialog(parentFrame, "Máscaras de Bits", false);
        ventanaMascara.setLayout(new BorderLayout());

        JSlider slider = new JSlider(1, 8, ultimoValorMascara);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createTitledBorder("Bits por Canal (1-8)"));
        slider.setPreferredSize(new Dimension(250, 70));

        slider.addChangeListener(e -> {
            ultimoValorMascara = slider.getValue();
            aplicarPrevisualizacionDinamica(new BitMaskFilter(ultimoValorMascara, 0.8f));
        });

        aplicarPrevisualizacionDinamica(new BitMaskFilter(ultimoValorMascara, 0.8f));

        JPanel panelBotones = crearBotonesDinamicos(activeFilterName);
        ventanaMascara.add(slider, BorderLayout.CENTER);
        ventanaMascara.add(panelBotones, BorderLayout.SOUTH);
        ventanaMascara.pack();
        posicionarVentana(ventanaMascara, 150);
    }

    private void accionAjusteRGB() {
        if (ventanaRGB != null && ventanaRGB.isVisible()) {
            ventanaRGB.toFront(); return;
        }

        activeFilterName = "Ajuste Tonalidad RGB";
        prepararEdicionDinamica(activeFilterName);

        ventanaRGB = new JDialog(parentFrame, "Ajuste RGB", false);
        ventanaRGB.setLayout(new BoxLayout(ventanaRGB.getContentPane(), BoxLayout.Y_AXIS));

        JSlider sliderR = crearSliderRGB("Rojo", -255, 255, valR);
        JSlider sliderG = crearSliderRGB("Verde", -255, 255, valG);
        JSlider sliderB = crearSliderRGB("Azul", -255, 255, valB);

        javax.swing.event.ChangeListener listener = e -> {
            valR = sliderR.getValue();
            valG = sliderG.getValue();
            valB = sliderB.getValue();
            aplicarPrevisualizacionDinamica(new RGBAdjustmentFilter(valR, valG, valB));
        };

        sliderR.addChangeListener(listener);
        sliderG.addChangeListener(listener);
        sliderB.addChangeListener(listener);

        aplicarPrevisualizacionDinamica(new RGBAdjustmentFilter(valR, valG, valB));

        ventanaRGB.add(sliderR);
        ventanaRGB.add(sliderG);
        ventanaRGB.add(sliderB);

        JPanel panelBotones = crearBotonesDinamicos(activeFilterName);
        ventanaRGB.add(panelBotones);

        ventanaRGB.pack();
        posicionarVentana(ventanaRGB, 230);
    }

    private void accionHistograma() {
        if (ventanaHistograma != null && ventanaHistograma.isVisible()) {
            ventanaHistograma.toFront(); return;
        }

        ventanaHistograma = new JDialog(parentFrame, "Histograma de Imagen", false);
        ventanaHistograma.setLayout(new BorderLayout());

        HistogramPanel histogramPanel = new HistogramPanel();
        histogramPanel.setImage(currentImage != null ? currentImage : originalImage);

        // Opciones de visualización
        JPanel panelOpciones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JRadioButton rbRGB = new JRadioButton("RGB Combinado", true);
        JRadioButton rbR = new JRadioButton("Solo Rojo");
        JRadioButton rbG = new JRadioButton("Solo Verde");
        JRadioButton rbB = new JRadioButton("Solo Azul");

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbRGB); bg.add(rbR); bg.add(rbG); bg.add(rbB);

        rbRGB.addActionListener(e -> histogramPanel.setMode(HistogramPanel.Mode.RGB));
        rbR.addActionListener(e -> histogramPanel.setMode(HistogramPanel.Mode.RED));
        rbG.addActionListener(e -> histogramPanel.setMode(HistogramPanel.Mode.GREEN));
        rbB.addActionListener(e -> histogramPanel.setMode(HistogramPanel.Mode.BLUE));

        panelOpciones.add(rbRGB);
        panelOpciones.add(rbR);
        panelOpciones.add(rbG);
        panelOpciones.add(rbB);

        ventanaHistograma.add(histogramPanel, BorderLayout.CENTER);
        ventanaHistograma.add(panelOpciones, BorderLayout.SOUTH);

        ventanaHistograma.pack();
        posicionarVentana(ventanaHistograma, 310);
    }

    private JSlider crearSliderRGB(String titulo, int min, int max, int initVal) {
        JSlider slider = new JSlider(min, max, initVal);
        slider.setMajorTickSpacing(128);
        slider.setPaintTicks(true);
        slider.setBorder(BorderFactory.createTitledBorder(titulo));
        return slider;
    }

    private JPanel crearBotonesDinamicos(String nombreFiltro) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> descartarHerramientaDinamica());

        JButton btnAplicar = new JButton("Aplicar");
        btnAplicar.setBackground(new Color(40, 160, 80));
        btnAplicar.setForeground(Color.WHITE);
        btnAplicar.addActionListener(e -> confirmarHerramientaDinamica(nombreFiltro));

        panel.add(btnCancelar);
        panel.add(btnAplicar);
        return panel;
    }

    private void posicionarVentana(JDialog dialog, int yOffset) {
        int xPos = parentFrame.getX() + parentFrame.getWidth() - dialog.getWidth() - 30;
        int yPos = parentFrame.getY() + yOffset;
        dialog.setLocation(xPos, yPos);
        dialog.setVisible(true);
    }

    private void accionAjusteHSV() {
        if (ventanaHSV != null && ventanaHSV.isVisible()) {
            ventanaHSV.toFront(); return;
        }

        activeFilterName = "Ajuste HSV";
        prepararEdicionDinamica(activeFilterName);

        ventanaHSV = new JDialog(parentFrame, "Ajuste HSV", false);
        ventanaHSV.setLayout(new BoxLayout(ventanaHSV.getContentPane(), BoxLayout.Y_AXIS));

        JSlider sliderH = new JSlider(-180, 180, valH);
        sliderH.setMajorTickSpacing(90);
        sliderH.setPaintTicks(true);
        sliderH.setBorder(BorderFactory.createTitledBorder("Tono (Hue) (-180° a 180°)"));

        JSlider sliderS = new JSlider(-100, 100, valS);
        sliderS.setMajorTickSpacing(50);
        sliderS.setPaintTicks(true);
        sliderS.setBorder(BorderFactory.createTitledBorder("Saturación (-100% a 100%)"));

        JSlider sliderV = new JSlider(-100, 100, valV);
        sliderV.setMajorTickSpacing(50);
        sliderV.setPaintTicks(true);
        sliderV.setBorder(BorderFactory.createTitledBorder("Brillo (Value) (-100% a 100%)"));

        javax.swing.event.ChangeListener listener = e -> {
            valH = sliderH.getValue();
            valS = sliderS.getValue();
            valV = sliderV.getValue();
            aplicarPrevisualizacionDinamica(new HSVAdjustmentFilter(valH / 360.0f, valS / 100.0f, valV / 100.0f));
        };

        sliderH.addChangeListener(listener);
        sliderS.addChangeListener(listener);
        sliderV.addChangeListener(listener);

        aplicarPrevisualizacionDinamica(new HSVAdjustmentFilter(valH / 360.0f, valS / 100.0f, valV / 100.0f));

        ventanaHSV.add(sliderH);
        ventanaHSV.add(sliderS);
        ventanaHSV.add(sliderV);

        JPanel panelBotones = crearBotonesDinamicos(activeFilterName);
        ventanaHSV.add(panelBotones);

        ventanaHSV.pack();
        posicionarVentana(ventanaHSV, 380);
    }

    private void accionMatricesColor() {
        if (ventanaMatrices != null && ventanaMatrices.isVisible()) {
            ventanaMatrices.toFront(); return;
        }

        activeFilterName = "Matriz de Color";
        prepararEdicionDinamica(activeFilterName);

        ventanaMatrices = new JDialog(parentFrame, "Matrices de Color", false);
        ventanaMatrices.setLayout(new BorderLayout());

        String[] presets = {"Neutro", "Sepia", "Vintage", "Polaroid", "Escala de Grises", "Invertir Colores", "Cálido", "Frío"};
        JComboBox<String> comboPresets = new JComboBox<>(presets);
        comboPresets.setSelectedIndex(ultimoPresetMatriz);
        comboPresets.setBorder(BorderFactory.createTitledBorder("Seleccionar Preset"));

        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 5, 5));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Coeficientes (4x5)"));
        JTextField[] fields = new JTextField[20];

        for (int i = 0; i < 20; i++) {
            fields[i] = new JTextField(String.format("%.3f", ultimoValorMatriz[i]));
            fields[i].setHorizontalAlignment(JTextField.CENTER);
            gridPanel.add(fields[i]);
        }

        Runnable updateMatrixPreview = () -> {
            try {
                for (int i = 0; i < 20; i++) {
                    String text = fields[i].getText().trim().replace(',', '.');
                    ultimoValorMatriz[i] = Float.parseFloat(text);
                }
                aplicarPrevisualizacionDinamica(new ColorMatrixFilter("Matriz de Color", ultimoValorMatriz));
            } catch (NumberFormatException ex) {
            }
        };

        java.awt.event.KeyAdapter keyListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateMatrixPreview.run();
            }
        };
        for (int i = 0; i < 20; i++) {
            fields[i].addKeyListener(keyListener);
        }

        comboPresets.addActionListener(e -> {
            ultimoPresetMatriz = comboPresets.getSelectedIndex();
            float[] selectedMat;
            switch (ultimoPresetMatriz) {
                case 1 -> selectedMat = ColorMatrixFilter.getSepia();
                case 2 -> selectedMat = ColorMatrixFilter.getVintage();
                case 3 -> selectedMat = ColorMatrixFilter.getPolaroid();
                case 4 -> selectedMat = ColorMatrixFilter.getGrayscale();
                case 5 -> selectedMat = ColorMatrixFilter.getInvert();
                case 6 -> selectedMat = ColorMatrixFilter.getWarm();
                case 7 -> selectedMat = ColorMatrixFilter.getCool();
                default -> selectedMat = ColorMatrixFilter.getNeutral();
            }
            for (int i = 0; i < 20; i++) {
                fields[i].setText(String.format("%.3f", selectedMat[i]));
            }
            updateMatrixPreview.run();
        });

        aplicarPrevisualizacionDinamica(new ColorMatrixFilter("Matriz de Color", ultimoValorMatriz));

        ventanaMatrices.add(comboPresets, BorderLayout.NORTH);
        ventanaMatrices.add(gridPanel, BorderLayout.CENTER);

        JPanel panelBotones = crearBotonesDinamicos(activeFilterName);
        ventanaMatrices.add(panelBotones, BorderLayout.SOUTH);

        ventanaMatrices.pack();
        posicionarVentana(ventanaMatrices, 450);
    }

    private void cerrarVentanasFlotantes() {
        if (ventanaTransparencia != null) ventanaTransparencia.setVisible(false);
        if (ventanaMascara != null) ventanaMascara.setVisible(false);
        if (ventanaRGB != null) ventanaRGB.setVisible(false);
        if (ventanaHSV != null) ventanaHSV.setVisible(false);
        if (ventanaMatrices != null) ventanaMatrices.setVisible(false);
        if (ventanaHistograma != null) ventanaHistograma.setVisible(false);
        
        if (previewImage != null || modificandoUltimoPaso) {
            descartarHerramientaDinamica();
        }
    }

    // Helper for deep copy
    private BufferedImage copyImage(BufferedImage source) {
        if (source == null) return null;
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }
}
