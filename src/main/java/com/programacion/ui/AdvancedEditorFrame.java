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

    private int ultimoValorTransparencia = 50;
    private int ultimoValorMascara = 4;
    private int valR = 0, valG = 0, valB = 0;
    private int valH = 0, valS = 0, valV = 0;

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
        toolBar.setMargin(new Insets(5, 10, 5, 10));

        btnVolver = new JButton("Volver al Editor Simple");
        btnVolver.addActionListener(e -> volverAlPrincipal());

        btnGuardar = new JButton("Guardar Resultado");
        btnGuardar.addActionListener(e -> accionGuardar());

        btnDeshacer = new JButton("Deshacer");
        btnDeshacer.addActionListener(e -> accionDeshacer());

        btnReiniciar = new JButton("Reiniciar Todo");
        btnReiniciar.addActionListener(e -> accionReiniciar());

        btnTema = new JButton("Tema");
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
        btnGuardar.setIcon(loadIcon("/assets/icons/save.png", 20));
        btnTema.setIcon(loadIcon("/assets/icons/theme.png", 20));
        // Se pueden añadir más iconos si existen (undo, reset, back)
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
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Filtros de Color
        sidebar.add(crearEncabezado("FILTROS DE COLOR", false));
        agregarBotonFiltro(sidebar, new GrayscaleFilter(255));
        agregarBotonFiltro(sidebar, new NegativeFilter());
        agregarBotonFiltro(sidebar, new BlackAndWhiteFilter());

        // Efectos Visuales
        sidebar.add(crearEncabezado("EFECTOS VISUALES", true));
        agregarBotonFiltro(sidebar, new FrostedGlassFilter());
        agregarBotonFiltro(sidebar, new CircularFadeFilter());

        // Convoluciones
        sidebar.add(crearEncabezado("ENFOQUE Y DESENFOQUE", true));
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
                comboConvolucion.setSelectedIndex(0); // Reset para poder seleccionarlo de nuevo
            }
        });
        sidebar.add(comboConvolucion);

        // Herramientas Dinámicas
        sidebar.add(crearEncabezado("HERRAMIENTAS DINÁMICAS", true));

        JButton btnTrans = new JButton("Transparencia Ajustable");
        btnTrans.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnTrans.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTrans.addActionListener(e -> accionTransparenciaAjustable());
        sidebar.add(btnTrans);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton btnMasc = new JButton("Máscaras de Bits");
        btnMasc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnMasc.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMasc.addActionListener(e -> accionMascaraBitsAdjustable());
        sidebar.add(btnMasc);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton btnRGB = new JButton("Tonalidades RGB");
        btnRGB.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnRGB.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRGB.setBackground(new Color(40, 120, 200));
        btnRGB.setForeground(Color.WHITE);
        btnRGB.addActionListener(e -> accionAjusteRGB());
        sidebar.add(btnRGB);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton btnHSV = new JButton("Ajuste HSV");
        btnHSV.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnHSV.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHSV.addActionListener(e -> accionAjusteHSV());
        sidebar.add(btnHSV);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton btnMatrices = new JButton("Matrices de Color");
        btnMatrices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnMatrices.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMatrices.addActionListener(e -> accionMatricesColor());
        sidebar.add(btnMatrices);

        // Análisis de Imagen
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(crearEncabezado("ANÁLISIS DE IMAGEN", true));

        JButton btnHistograma = new JButton("Ver Histograma");
        btnHistograma.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnHistograma.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHistograma.setBackground(new Color(120, 80, 160));
        btnHistograma.setForeground(Color.WHITE);
        btnHistograma.addActionListener(e -> accionHistograma());
        sidebar.add(btnHistograma);

        // Historial de Efectos
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(crearEncabezado("HISTORIAL DE EFECTOS", true));

        listHistorial = new JList<>(modelHistorial);
        listHistorial.setEnabled(false); // Solo visual
        listHistorial.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane scrollHistorial = new JScrollPane(listHistorial);
        scrollHistorial.setPreferredSize(new Dimension(250, 120));
        scrollHistorial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        scrollHistorial.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        sidebar.add(scrollHistorial);

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

    private void confirmarHerramientaDinamica(String nombreFiltro) {
        if (previewImage != null) {
            history.push(copyImage(currentImage));
            filterNames.push(nombreFiltro);
            currentImage = copyImage(previewImage);
            previewImage = null;
            mostrarImagen(currentImage);
            actualizarHistorialVista();
        }
        cerrarVentanasFlotantes();
    }

    private void descartarHerramientaDinamica() {
        previewImage = null;
        mostrarImagen(currentImage);
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

        JPanel panelBotones = crearBotonesDinamicos("Transparencia Ajustable");
        ventanaTransparencia.add(slider, BorderLayout.CENTER);
        ventanaTransparencia.add(panelBotones, BorderLayout.SOUTH);
        ventanaTransparencia.pack();
        posicionarVentana(ventanaTransparencia, 70);
    }

    private void accionMascaraBitsAdjustable() {
        if (ventanaMascara != null && ventanaMascara.isVisible()) {
            ventanaMascara.toFront(); return;
        }

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

        JPanel panelBotones = crearBotonesDinamicos("Máscaras de Bits");
        ventanaMascara.add(slider, BorderLayout.CENTER);
        ventanaMascara.add(panelBotones, BorderLayout.SOUTH);
        ventanaMascara.pack();
        posicionarVentana(ventanaMascara, 150);
    }

    private void accionAjusteRGB() {
        if (ventanaRGB != null && ventanaRGB.isVisible()) {
            ventanaRGB.toFront(); return;
        }

        ventanaRGB = new JDialog(parentFrame, "Ajuste RGB", false);
        ventanaRGB.setLayout(new BoxLayout(ventanaRGB.getContentPane(), BoxLayout.Y_AXIS));

        // Reset local values for new session
        valR = 0; valG = 0; valB = 0;

        JSlider sliderR = crearSliderRGB("Rojo", -255, 255);
        JSlider sliderG = crearSliderRGB("Verde", -255, 255);
        JSlider sliderB = crearSliderRGB("Azul", -255, 255);

        javax.swing.event.ChangeListener listener = e -> {
            valR = sliderR.getValue();
            valG = sliderG.getValue();
            valB = sliderB.getValue();
            aplicarPrevisualizacionDinamica(new RGBAdjustmentFilter(valR, valG, valB));
        };

        sliderR.addChangeListener(listener);
        sliderG.addChangeListener(listener);
        sliderB.addChangeListener(listener);

        ventanaRGB.add(sliderR);
        ventanaRGB.add(sliderG);
        ventanaRGB.add(sliderB);

        JPanel panelBotones = crearBotonesDinamicos("Ajuste Tonalidad RGB");
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

    private JSlider crearSliderRGB(String titulo, int min, int max) {
        JSlider slider = new JSlider(min, max, 0);
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

        ventanaHSV = new JDialog(parentFrame, "Ajuste HSV", false);
        ventanaHSV.setLayout(new BoxLayout(ventanaHSV.getContentPane(), BoxLayout.Y_AXIS));

        valH = 0; valS = 0; valV = 0;

        JSlider sliderH = new JSlider(-180, 180, 0);
        sliderH.setMajorTickSpacing(90);
        sliderH.setPaintTicks(true);
        sliderH.setBorder(BorderFactory.createTitledBorder("Tono (Hue) (-180° a 180°)"));

        JSlider sliderS = new JSlider(-100, 100, 0);
        sliderS.setMajorTickSpacing(50);
        sliderS.setPaintTicks(true);
        sliderS.setBorder(BorderFactory.createTitledBorder("Saturación (-100% a 100%)"));

        JSlider sliderV = new JSlider(-100, 100, 0);
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

        ventanaHSV.add(sliderH);
        ventanaHSV.add(sliderS);
        ventanaHSV.add(sliderV);

        JPanel panelBotones = crearBotonesDinamicos("Ajuste HSV");
        ventanaHSV.add(panelBotones);

        ventanaHSV.pack();
        posicionarVentana(ventanaHSV, 380);
    }

    private void accionMatricesColor() {
        if (ventanaMatrices != null && ventanaMatrices.isVisible()) {
            ventanaMatrices.toFront(); return;
        }

        ventanaMatrices = new JDialog(parentFrame, "Matrices de Color", false);
        ventanaMatrices.setLayout(new BorderLayout());

        String[] presets = {"Neutro", "Sepia", "Vintage", "Polaroid", "Escala de Grises", "Invertir Colores", "Cálido", "Frío"};
        JComboBox<String> comboPresets = new JComboBox<>(presets);
        comboPresets.setBorder(BorderFactory.createTitledBorder("Seleccionar Preset"));

        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 5, 5));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Coeficientes (4x5)"));
        JTextField[] fields = new JTextField[20];

        float[] currentMatrix = ColorMatrixFilter.getNeutral().clone();

        for (int i = 0; i < 20; i++) {
            fields[i] = new JTextField(String.format("%.3f", currentMatrix[i]));
            fields[i].setHorizontalAlignment(JTextField.CENTER);
            gridPanel.add(fields[i]);
        }

        Runnable updateMatrixPreview = () -> {
            float[] mat = new float[20];
            try {
                for (int i = 0; i < 20; i++) {
                    String text = fields[i].getText().trim().replace(',', '.');
                    mat[i] = Float.parseFloat(text);
                }
                aplicarPrevisualizacionDinamica(new ColorMatrixFilter("Matriz de Color", mat));
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
            float[] selectedMat;
            switch (comboPresets.getSelectedIndex()) {
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

        ventanaMatrices.add(comboPresets, BorderLayout.NORTH);
        ventanaMatrices.add(gridPanel, BorderLayout.CENTER);

        JPanel panelBotones = crearBotonesDinamicos("Matriz de Color");
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
        // Si había una previsualización activa y el usuario cerró la ventana de golpe, se descarta.
        if (previewImage != null) {
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
