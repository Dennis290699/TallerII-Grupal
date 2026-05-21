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
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage filteredImage;
    private ImagePreviewPanel previewPanel = new ImagePreviewPanel();
    private JScrollPane scrollMain;
    private JLayeredPane layeredPane;
    private HistogramPanel miniHistogramPanel;
    private boolean isDarkMode = true;
    private Container simpleModeContentPane;

    private int ultimoValorTransparencia = 50;
    private JDialog ventanaTransparencia = null;
    private int ultimoValorMascara = 4;
    private JDialog ventanaMascara = null;
    private JDialog ventanaHistograma = null;
    private JDialog ventanaHSV = null;
    private JDialog ventanaMatrices = null;
    private int ultimoHueHSV = 0;
    private int ultimoSatHSV = 0;
    private int ultimoValHSV = 0;

    private JButton btnCargar, btnLimpiar, btnGuardar, btnTema, btnVerOriginal, btnModoApilado, btnBlendingMulticapa;

    public MainFrame() {
        setTitle("Editor de Imágenes Universitario");
        setSize(1100, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initToolbar();
        initWorkspace();
        initRightSidebar();

        updateIcons();
    }

    private void initToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 10, 5, 10));

        btnCargar = new JButton("Cargar");
        btnCargar.addActionListener(e -> accionCargar());

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> accionLimpiar());

        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> accionGuardar());

        btnVerOriginal = new JButton("Mantener para ver Original");
        btnVerOriginal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerOriginal.addMouseListener(new MouseAdapter() {
            private Component previousView;

            @Override
            public void mousePressed(MouseEvent e) {
                if (originalImage != null) {
                    previousView = scrollMain.getViewport().getView();
                    if (previousView == previewPanel) {
                        previewPanel.setImage(originalImage);
                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (filteredImage != null) {
                    if (previousView == previewPanel) {
                        previewPanel.setImage(filteredImage);
                    }
                }
            }
        });

        btnTema = new JButton("Tema");
        btnTema.addActionListener(e -> accionCambiarTema());

        btnModoApilado = new JButton("Modo Apilado (Avanzado)");
        btnModoApilado.addActionListener(e -> abrirEditorAvanzado());

        btnBlendingMulticapa = new JButton("Blending Multicapa");
        btnBlendingMulticapa.addActionListener(e -> abrirMezcladorBlending());

        toolBar.add(btnCargar);
        toolBar.addSeparator();
        toolBar.add(btnLimpiar);
        toolBar.addSeparator();
        toolBar.add(btnGuardar);
        toolBar.addSeparator();
        toolBar.add(btnVerOriginal);
        toolBar.addSeparator();
        toolBar.add(btnModoApilado);
        toolBar.addSeparator();
        toolBar.add(btnBlendingMulticapa);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnTema);

        add(toolBar, BorderLayout.NORTH);
    }

    private void updateIcons() {
        btnCargar.setIcon(loadIcon("/assets/icons/add.png", 20));
        btnGuardar.setIcon(loadIcon("/assets/icons/save.png", 20));
        btnTema.setIcon(loadIcon("/assets/icons/theme.png", 20));
        btnVerOriginal.setIcon(loadIcon("/assets/icons/eye.png", 20)); // Añade un icono de ojo si tienes
        ImageIcon clearIcon = loadIcon("/assets/icons/delete.png", 20);
        if (clearIcon != null) btnLimpiar.setIcon(clearIcon);
        
        ImageIcon blendIcon = loadIcon("/assets/icons/clear-filter.png", 20);
        if (blendIcon != null && btnBlendingMulticapa != null) {
            btnBlendingMulticapa.setIcon(blendIcon);
        }
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

    // --- WORKSPACE UNIFICADO ---
    private void initWorkspace() {
        layeredPane = new JLayeredPane();

        scrollMain = new JScrollPane(previewPanel);
        scrollMain.setBorder(BorderFactory.createEmptyBorder()); // Elimina bordes para un look más limpio

        miniHistogramPanel = new HistogramPanel();
        miniHistogramPanel.setOverlay(true);
        miniHistogramPanel.setMode(HistogramPanel.Mode.RGB);
        miniHistogramPanel.setVisible(false); // Oculto hasta que se cargue imagen

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

    // --- BARRA LATERAL DERECHA (ESTILO LIGHTROOM) ---
    private void initRightSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sección: Filtros de Color
        sidebar.add(crearEncabezado("FILTROS DE COLOR", false));
        agregarBotonFiltro(sidebar, new GrayscaleFilter(255));
        agregarBotonFiltro(sidebar, new NegativeFilter());
        agregarBotonFiltro(sidebar, new BlackAndWhiteFilter());

        // Sección: Efectos Especiales
        sidebar.add(crearEncabezado("EFECTOS VISUALES", true));
        agregarBotonFiltro(sidebar, new FrostedGlassFilter());
        agregarBotonFiltro(sidebar, new CircularFadeFilter());

        // Sección: Convoluciones (En un combo para no saturar)
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
            }
        });
        sidebar.add(comboConvolucion);

        // Sección: Herramientas Dinámicas
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

        // Sección: Análisis
        sidebar.add(crearEncabezado("VISTAS DE ANÁLISIS", true));

        JButton btnHistograma = new JButton("Ver Histograma");
        btnHistograma.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnHistograma.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnHistograma.setBackground(new Color(120, 80, 160));
        btnHistograma.setForeground(Color.WHITE);
        btnHistograma.addActionListener(e -> accionHistograma());
        sidebar.add(btnHistograma);
        sidebar.add(Box.createRigidArea(new Dimension(0, 5)));

        String[] analisis = {
            "Bits", "Canales", "Retro 1", "Retro 2", "Radiales", "Estiramiento", "Convoluciones"
        };
        JComboBox<String> comboAnalisis = new JComboBox<>();
        comboAnalisis.addItem("Seleccionar comparativa...");
        for (String a : analisis) comboAnalisis.addItem(a);
        comboAnalisis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        comboAnalisis.setAlignmentX(Component.CENTER_ALIGNMENT);
        comboAnalisis.addActionListener(e -> ejecutarAnalisis(comboAnalisis.getSelectedIndex()));
        sidebar.add(comboAnalisis);

        // Spacer y Reset
        sidebar.add(Box.createVerticalGlue());
        JButton btnReset = new JButton("Restaurar Imagen");
        btnReset.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnReset.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReset.setBackground(new Color(220, 50, 50));
        btnReset.setForeground(Color.WHITE);
        btnReset.addActionListener(e -> accionReset());
        sidebar.add(btnReset);

        JScrollPane scrollSidebar = new JScrollPane(sidebar);
        scrollSidebar.setPreferredSize(new Dimension(280, 0));
        scrollSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Component.borderColor"))); // Borde sutil adaptable
        add(scrollSidebar, BorderLayout.EAST); // Movido a la derecha
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
        btn.putClientProperty("JButton.buttonType", "roundRect"); // Estilo FlatLaf
        btn.addActionListener(e -> {
            cerrarVentanasFlotantes();
            aplicarFiltro(filtro);
        });
        contenedor.add(btn);
        contenedor.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private ImageIcon prepararImagenParaLabel(BufferedImage img, boolean esComparativa) {
        if (img == null) return null;
        int targetWidth = esComparativa ? 250 : scrollMain.getWidth() - 20;
        if (targetWidth <= 0) targetWidth = 800; // Fallback
        double ratio = (double) img.getHeight() / img.getWidth();
        int targetHeight = (int) (targetWidth * ratio);
        Image escalada = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(escalada);
    }

    private void accionCargar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "png", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(chooser.getSelectedFile());
                filteredImage = originalImage;
                previewPanel.setImage(filteredImage);
                scrollMain.setViewportView(previewPanel);
                notificarHistograma();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar: " + ex.getMessage());
            }
        }
    }

    private void accionLimpiar() {
        cerrarVentanasFlotantes();
        originalImage = null;
        filteredImage = null;
        previewPanel.setImage(null);
        scrollMain.setViewportView(previewPanel);
        if (miniHistogramPanel != null) {
            miniHistogramPanel.setVisible(false);
        }
    }

    private void aplicarFiltro(ImageFilter filtro) {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            BufferedImage result = filtro.apply(originalImage);
            SwingUtilities.invokeLater(() -> {
                filteredImage = result;
                previewPanel.setImage(filteredImage);
                scrollMain.setViewportView(previewPanel); // Retorna a la vista de una imagen
                notificarHistograma();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionReset() {
        if (originalImage != null) {
            cerrarVentanasFlotantes();
            filteredImage = originalImage;
            previewPanel.setImage(originalImage);
            scrollMain.setViewportView(previewPanel);
            notificarHistograma();
        }
    }

    private void notificarHistograma() {
        if (filteredImage != null) {
            if (miniHistogramPanel != null) {
                miniHistogramPanel.setImage(filteredImage);
                miniHistogramPanel.setVisible(true);
            }
        }

        if (ventanaHistograma != null && ventanaHistograma.isVisible()) {
            for (Component c : ventanaHistograma.getContentPane().getComponents()) {
                if (c instanceof HistogramPanel) {
                    ((HistogramPanel) c).setImage(filteredImage);
                    break;
                }
            }
        }
    }

    private void ejecutarAnalisis(int index) {
        cerrarVentanasFlotantes();
        // Redirige al método correspondiente según el ComboBox de análisis
        switch (index) {
            case 1 -> accionComparativaBits();
            case 2 -> accionComparativaCanales();
            case 3 -> accionComparativaRetro1();
            case 4 -> accionComparativaRetro2();
            case 5 -> accionComparativaRadiales();
            case 6 -> accionComparativaEstiramiento();
            case 7 -> accionComparativaConvoluciones();
        }
    }

    // --- CÓDIGO ORIGINAL INTEGRADO ---

    private void accionComparativaBits() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] niveles = { 2, 4, 8, 64, 128, 255 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1200));

            for (int n : niveles) {
                GrayscaleFilter f = new GrayscaleFilter(n);
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                JLabel imgLabel = new JLabel(prepararImagenParaLabel(imgResult, true));
                JLabel infoLabel = new JLabel("Nivel Gris N = " + n, SwingConstants.CENTER);
                infoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

                item.add(imgLabel, BorderLayout.CENTER);
                item.add(infoLabel, BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());

                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaCanales() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] tipos = { 1, 2, 3, 4, 5 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1200));

            for (int t : tipos) {
                ColorChannelFilter f = new ColorChannelFilter(t);
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                JLabel imgLabel = new JLabel(prepararImagenParaLabel(imgResult, true));
                JLabel infoLabel = new JLabel(f.getName(), SwingConstants.CENTER);
                infoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

                item.add(imgLabel, BorderLayout.CENTER);
                item.add(infoLabel, BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());

                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaRetro1() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] niveles = { 2, 4, 8, 64, 128, 255 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1200));

            for (int n : niveles) {
                RetroEffectFilter f = new RetroEffectFilter(n);
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                JLabel imgLabel = new JLabel(prepararImagenParaLabel(imgResult, true));
                JLabel infoLabel = new JLabel("Retro 1 (RGB) N = " + n, SwingConstants.CENTER);
                infoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

                item.add(imgLabel, BorderLayout.CENTER);
                item.add(infoLabel, BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());

                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaRetro2() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] niveles = { 2, 4, 8, 64, 128, 255 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1200));

            for (int n : niveles) {
                RetroTwoFilter f = new RetroTwoFilter(n, 1);
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                JLabel imgLabel = new JLabel(prepararImagenParaLabel(imgResult, true));
                JLabel infoLabel = new JLabel("Retro 2 (RG) N = " + n, SwingConstants.CENTER);
                infoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

                item.add(imgLabel, BorderLayout.CENTER);
                item.add(infoLabel, BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());

                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaRadiales() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] tipos = { 1, 2, 3, 4, 5 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1200));

            for (int t : tipos) {
                RadialGradientFilter f = new RadialGradientFilter(t);
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                JLabel imgLabel = new JLabel(prepararImagenParaLabel(imgResult, true));
                JLabel infoLabel = new JLabel(f.getName(), SwingConstants.CENTER);
                infoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

                item.add(imgLabel, BorderLayout.CENTER);
                item.add(infoLabel, BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());

                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaEstiramiento() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            int[] bitsArr = { 2, 4, 8 };
            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1500));

            for (int b : bitsArr) {
                StretchingFilter fRGB = new StretchingFilter(b, 1);
                BufferedImage imgRGB = fRGB.apply(originalImage);

                JPanel itemRGB = new JPanel(new BorderLayout());
                itemRGB.add(new JLabel(prepararImagenParaLabel(imgRGB, true)), BorderLayout.CENTER);
                itemRGB.add(new JLabel(fRGB.getName(), SwingConstants.CENTER), BorderLayout.SOUTH);
                itemRGB.setBorder(BorderFactory.createEtchedBorder());
                panelGrid.add(itemRGB);

                StretchingFilter fHSV = new StretchingFilter(b, 2);
                BufferedImage imgHSV = fHSV.apply(originalImage);

                JPanel itemHSV = new JPanel(new BorderLayout());
                itemHSV.add(new JLabel(prepararImagenParaLabel(imgHSV, true)), BorderLayout.CENTER);
                itemHSV.add(new JLabel(fHSV.getName(), SwingConstants.CENTER), BorderLayout.SOUTH);
                itemHSV.setBorder(BorderFactory.createEtchedBorder());
                panelGrid.add(itemHSV);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionComparativaConvoluciones() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            ImageFilter[] convs = {
                    ConvolutionFilter.Enfoque(),
                    ConvolutionFilter.Desenfoque(),
                    ConvolutionFilter.DesenfoquePesado(),
                    ConvolutionFilter.Bordes(),
                    ConvolutionFilter.Aclarar(),
                    ConvolutionFilter.Oscurecer()
            };

            JPanel panelGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            panelGrid.setPreferredSize(new Dimension(900, 1500));

            for (ImageFilter f : convs) {
                BufferedImage imgResult = f.apply(originalImage);

                JPanel item = new JPanel(new BorderLayout());
                item.add(new JLabel(prepararImagenParaLabel(imgResult, true)), BorderLayout.CENTER);
                item.add(new JLabel(f.getName(), SwingConstants.CENTER), BorderLayout.SOUTH);
                item.setBorder(BorderFactory.createEtchedBorder());
                panelGrid.add(item);
            }

            SwingUtilities.invokeLater(() -> {
                scrollMain.setViewportView(panelGrid);
                revalidate();
                repaint();
                setCursor(Cursor.getDefaultCursor());
            });
        }).start();
    }

    private void accionGuardar() {
        Component view = scrollMain.getViewport().getView();
        if (view == null) return;

        BufferedImage imageToSave = null;

        // Determinar qué estamos viendo actualmente
        if (view instanceof JPanel && view != previewPanel) {
            // Es una vista de análisis (Grid)
            JPanel panel = (JPanel) view;
            imageToSave = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = imageToSave.createGraphics();
            panel.paint(g2d);
            g2d.dispose();
        } else if (view == previewPanel && filteredImage != null) {
            // Es la vista normal de imagen filtrada
            imageToSave = filteredImage;
        }

        if (imageToSave == null) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File output = chooser.getSelectedFile();
                if (!output.getName().toLowerCase().endsWith(".png")) {
                    output = new File(output.getAbsolutePath() + ".png");
                }
                ImageIO.write(imageToSave, "png", output);
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
            SwingUtilities.updateComponentTreeUI(this);
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
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception e) {}
        }
    }

    public void mostrarSimpleMode() {
        if (simpleModeContentPane != null) {
            setContentPane(simpleModeContentPane);
            setTitle("Editor de Imágenes Universitario");
            revalidate();
            repaint();
        }
    }

    private void abrirEditorAvanzado() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        cerrarVentanasFlotantes();
        if (simpleModeContentPane == null) {
            simpleModeContentPane = getContentPane();
        }
        AdvancedEditorFrame advancedPanel = new AdvancedEditorFrame(this, originalImage, isDarkMode);
        setContentPane(advancedPanel);
        setTitle("Editor Avanzado - Modo Apilado");
        revalidate();
        repaint();
    }

    private void abrirMezcladorBlending() {
        cerrarVentanasFlotantes();
        if (simpleModeContentPane == null) {
            simpleModeContentPane = getContentPane();
        }
        BlendingFrame blendingPanel = new BlendingFrame(this, isDarkMode);
        setContentPane(blendingPanel);
        setTitle("Mezclador Blending Multicapa");
        revalidate();
        repaint();
    }

    private void accionTransparenciaAjustable() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }

        if (ventanaTransparencia != null && ventanaTransparencia.isVisible()) {
            ventanaTransparencia.toFront();
            return;
        }

        ventanaTransparencia = new JDialog(this, "Nivel de Transparencia", false);
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
            aplicarFiltro(new TransparenciaFilter(factor));
        });

        aplicarFiltro(new TransparenciaFilter(ultimoValorTransparencia / 100.0f));

        ventanaTransparencia.add(slider, BorderLayout.CENTER);
        ventanaTransparencia.pack();

        int xPos = this.getX() + this.getWidth() - ventanaTransparencia.getWidth() - 30;
        int yPos = this.getY() + 70;
        ventanaTransparencia.setLocation(xPos, yPos);
        ventanaTransparencia.setVisible(true);
    }

    private void accionMascaraBitsAdjustable() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }

        if (ventanaMascara != null && ventanaMascara.isVisible()) {
            ventanaMascara.toFront();
            return;
        }

        ventanaMascara = new JDialog(this, "Filtro de Máscaras", false);
        ventanaMascara.setLayout(new BorderLayout());

        JSlider slider = new JSlider(1, 8, ultimoValorMascara);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createTitledBorder("Bits por Canal (1-8)"));
        slider.setPreferredSize(new Dimension(250, 70));

        slider.addChangeListener(e -> {
            ultimoValorMascara = slider.getValue();
            aplicarFiltro(new BitMaskFilter(ultimoValorMascara, 0.8f));
        });

        aplicarFiltro(new BitMaskFilter(ultimoValorMascara, 0.8f));

        ventanaMascara.add(slider, BorderLayout.CENTER);
        ventanaMascara.pack();

        int xPos = this.getX() + this.getWidth() - ventanaMascara.getWidth() - 30;
        int yPos = this.getY() + 150;
        ventanaMascara.setLocation(xPos, yPos);
        ventanaMascara.setVisible(true);
    }

    private void accionHistograma() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }
        if (ventanaHistograma != null && ventanaHistograma.isVisible()) {
            ventanaHistograma.toFront(); return;
        }

        ventanaHistograma = new JDialog(this, "Histograma de Imagen", false);
        ventanaHistograma.setLayout(new BorderLayout());

        HistogramPanel histogramPanel = new HistogramPanel();
        histogramPanel.setImage(filteredImage != null ? filteredImage : originalImage);

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

        int xPos = this.getX() + this.getWidth() - ventanaHistograma.getWidth() - 30;
        int yPos = this.getY() + 230;
        ventanaHistograma.setLocation(xPos, yPos);
        ventanaHistograma.setVisible(true);
    }

    private void accionAjusteHSV() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }

        if (ventanaHSV != null && ventanaHSV.isVisible()) {
            ventanaHSV.toFront();
            return;
        }

        ventanaHSV = new JDialog(this, "Ajuste HSV (Tono, Sat, Brillo)", false);
        ventanaHSV.setLayout(new BoxLayout(ventanaHSV.getContentPane(), BoxLayout.Y_AXIS));

        JSlider sliderH = new JSlider(-180, 180, ultimoHueHSV);
        sliderH.setMajorTickSpacing(90);
        sliderH.setPaintTicks(true);
        sliderH.setPaintLabels(true);
        sliderH.setBorder(BorderFactory.createTitledBorder("Tono (Hue) (-180° a 180°)"));

        JSlider sliderS = new JSlider(-100, 100, ultimoSatHSV);
        sliderS.setMajorTickSpacing(50);
        sliderS.setPaintTicks(true);
        sliderS.setPaintLabels(true);
        sliderS.setBorder(BorderFactory.createTitledBorder("Saturación (-100% a 100%)"));

        JSlider sliderV = new JSlider(-100, 100, ultimoValHSV);
        sliderV.setMajorTickSpacing(50);
        sliderV.setPaintTicks(true);
        sliderV.setPaintLabels(true);
        sliderV.setBorder(BorderFactory.createTitledBorder("Brillo (Value) (-100% a 100%)"));

        javax.swing.event.ChangeListener listener = e -> {
            ultimoHueHSV = sliderH.getValue();
            ultimoSatHSV = sliderS.getValue();
            ultimoValHSV = sliderV.getValue();
            aplicarFiltro(new HSVAdjustmentFilter(ultimoHueHSV / 360.0f, ultimoSatHSV / 100.0f, ultimoValHSV / 100.0f));
        };

        sliderH.addChangeListener(listener);
        sliderS.addChangeListener(listener);
        sliderV.addChangeListener(listener);

        ventanaHSV.add(sliderH);
        ventanaHSV.add(sliderS);
        ventanaHSV.add(sliderV);
        ventanaHSV.pack();

        int xPos = this.getX() + this.getWidth() - ventanaHSV.getWidth() - 30;
        int yPos = this.getY() + 310;
        ventanaHSV.setLocation(xPos, yPos);
        ventanaHSV.setVisible(true);
    }

    private void accionMatricesColor() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Carga una imagen primero.");
            return;
        }

        if (ventanaMatrices != null && ventanaMatrices.isVisible()) {
            ventanaMatrices.toFront();
            return;
        }

        ventanaMatrices = new JDialog(this, "Matrices de Color", false);
        ventanaMatrices.setLayout(new BorderLayout());

        String[] presets = {"Neutro", "Sepia", "Vintage", "Polaroid", "Escala de Grises", "Invertir Colores", "Cálido", "Frío"};
        JComboBox<String> comboPresets = new JComboBox<>(presets);
        comboPresets.setBorder(BorderFactory.createTitledBorder("Seleccionar Preset"));

        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 5, 5));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Coeficientes de la Matriz (4x5)"));
        JTextField[] fields = new JTextField[20];

        float[] currentMatrix = ColorMatrixFilter.getNeutral().clone();

        for (int i = 0; i < 20; i++) {
            fields[i] = new JTextField(String.format("%.3f", currentMatrix[i]));
            fields[i].setHorizontalAlignment(JTextField.CENTER);
            gridPanel.add(fields[i]);
        }

        Runnable applyMatrixFilter = () -> {
            float[] mat = new float[20];
            try {
                for (int i = 0; i < 20; i++) {
                    String text = fields[i].getText().trim().replace(',', '.');
                    mat[i] = Float.parseFloat(text);
                }
                aplicarFiltro(new ColorMatrixFilter("Matriz de Color", mat));
            } catch (NumberFormatException ex) {
            }
        };

        java.awt.event.KeyAdapter keyListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyMatrixFilter.run();
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
            applyMatrixFilter.run();
        });

        ventanaMatrices.add(comboPresets, BorderLayout.NORTH);
        ventanaMatrices.add(gridPanel, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> ventanaMatrices.setVisible(false));
        JPanel panelSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelSouth.add(btnCerrar);
        ventanaMatrices.add(panelSouth, BorderLayout.SOUTH);

        ventanaMatrices.pack();
        int xPos = this.getX() + this.getWidth() - ventanaMatrices.getWidth() - 30;
        int yPos = this.getY() + 380;
        ventanaMatrices.setLocation(xPos, yPos);
        ventanaMatrices.setVisible(true);
    }

    private void cerrarVentanasFlotantes() {
        if (ventanaTransparencia != null) ventanaTransparencia.setVisible(false);
        if (ventanaMascara != null) ventanaMascara.setVisible(false);
        if (ventanaHSV != null) ventanaHSV.setVisible(false);
        if (ventanaMatrices != null) ventanaMatrices.setVisible(false);
        if (ventanaHistograma != null) ventanaHistograma.setVisible(false);
    }
}
