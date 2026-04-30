package com.programacion.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.programacion.core.ImageFilter;
import com.programacion.filters.GrayscaleFilter;
import com.programacion.filters.NegativeFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage currentFilteredImage;

    private JLabel originalImageLabel;
    private JLabel filteredImageLabel;
    private boolean isDarkMode = true; // Rastrea el estado del tema

    // Componentes que necesitan actualizar su ícono al cambiar el tema
    private JButton btnToolbarLoad;
    private JButton btnToolbarSave;
    private JButton btnToolbarTheme;
    private JButton btnSidebarReset;
    private JMenuItem itemLoad;
    private JMenuItem itemSave;
    private JMenuItem itemTheme;

    public MainFrame() {
        setTitle("Editor Pro de Imágenes");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        buildMenuBar();
        buildToolBar();
        buildSidebar();
        buildMainWorkspace();

        // Configurar íconos iniciales según el tema activo
        updateIcons();
    }

    // --- 1. MENÚ SUPERIOR ---
    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menú Archivo
        JMenu fileMenu = new JMenu("Archivo");
        itemLoad = new JMenuItem("Cargar Imagen...");
        itemSave = new JMenuItem("Guardar Imagen Como...");

        itemLoad.addActionListener(e -> loadImage());
        itemSave.addActionListener(e -> saveImage());

        fileMenu.add(itemLoad);
        fileMenu.add(itemSave);

        // Menú Vista (Para el tema claro/oscuro)
        JMenu viewMenu = new JMenu("Vista");
        itemTheme = new JMenuItem("Alternar Tema (Claro / Oscuro)");
        itemTheme.addActionListener(e -> toggleTheme());
        viewMenu.add(itemTheme);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    // --- 1.5 BARRA DE HERRAMIENTAS (NUEVO) ---
    private void buildToolBar() {
        JToolBar toolBar = new JToolBar("Herramientas Principales");
        toolBar.setFloatable(false); // Fija la barra en la parte superior

        btnToolbarLoad = new JButton("Cargar");
        btnToolbarLoad.setToolTipText("Cargar nueva imagen");
        btnToolbarLoad.addActionListener(e -> loadImage());

        btnToolbarSave = new JButton("Guardar");
        btnToolbarSave.setToolTipText("Guardar imagen actual");
        btnToolbarSave.addActionListener(e -> saveImage());

        btnToolbarTheme = new JButton("Tema");
        btnToolbarTheme.setToolTipText("Cambiar entre tema claro y oscuro");
        btnToolbarTheme.addActionListener(e -> toggleTheme());

        // Añadimos algunos márgenes a los botones para que se vean mejor
        btnToolbarLoad.setMargin(new Insets(4, 10, 4, 10));
        btnToolbarSave.setMargin(new Insets(4, 10, 4, 10));
        btnToolbarTheme.setMargin(new Insets(4, 10, 4, 10));

        toolBar.add(btnToolbarLoad);
        toolBar.addSeparator();
        toolBar.add(btnToolbarSave);
        toolBar.addSeparator();
        toolBar.add(Box.createHorizontalGlue()); // Empuja el botón de tema a la derecha opcionalmente, pero lo dejamos junto
        toolBar.add(btnToolbarTheme);

        add(toolBar, BorderLayout.NORTH);
    }

    // --- 2. PANEL LATERAL (Filtros escalables) ---
    private void buildSidebar() {
        // Usamos un panel con disposición vertical (BoxLayout)
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Filtros Disponibles");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        filterPanel.add(titleLabel);
        filterPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Aquí agregas todos los filtros que vayas creando en el futuro
        List<ImageFilter> availableFilters = new ArrayList<>();
        availableFilters.add(new GrayscaleFilter());
        availableFilters.add(new NegativeFilter());
        // availableFilters.add(new SepiaFilter()); // <- Fácil de escalar

        // Generar botones dinámicamente según los filtros que existan
        for (ImageFilter filter : availableFilters) {
            JButton btnFilter = new JButton(filter.getName());
            btnFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnFilter.setMaximumSize(new Dimension(200, 35)); // Botones uniformes

            btnFilter.addActionListener(e -> applyFilter(filter));

            filterPanel.add(btnFilter);
            filterPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Espaciado
        }

        // Botón de reseteo
        filterPanel.add(Box.createVerticalGlue()); // Empuja el botón al fondo
        btnSidebarReset = new JButton("Limpiar Filtros");
        btnSidebarReset.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSidebarReset.setMaximumSize(new Dimension(200, 35));
        btnSidebarReset.addActionListener(e -> resetImage());
        filterPanel.add(btnSidebarReset);

        // Envolvemos el panel en un ScrollPane por si llegamos a tener 50 filtros
        JScrollPane scrollSidebar = new JScrollPane(filterPanel);
        scrollSidebar.setPreferredSize(new Dimension(220, 0));
        add(scrollSidebar, BorderLayout.WEST);
    }

    // --- 3. ÁREA CENTRAL (Vista Doble: Arriba / Abajo) ---
    private void buildMainWorkspace() {
        originalImageLabel = new JLabel("Original", SwingConstants.CENTER);
        filteredImageLabel = new JLabel("Filtrada", SwingConstants.CENTER);

        // JSplitPane con división VERTICAL
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Asignamos los componentes arriba y abajo
        splitPane.setTopComponent(new JScrollPane(originalImageLabel));
        splitPane.setBottomComponent(new JScrollPane(filteredImageLabel));

        // 50% de espacio para cada imagen al inicio
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        // Opcional: Hacer el divisor un poco más grueso para facilitar el agarre
        splitPane.setDividerSize(8);

        add(splitPane, BorderLayout.CENTER);
    }

    // --- MÉTODOS DE ICONOS Y TEMAS ---

    private ImageIcon loadIcon(String path, int size) {
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                return null; // Si no encuentra el ícono no rompe nada
            }
            BufferedImage img = ImageIO.read(imgUrl);
            
            // Si es modo claro, invertimos el color de los íconos (asumiendo que son blancos)
            if (!isDarkMode) {
                img = invertImageColors(img);
            }
            
            Image resized = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el ícono: " + path);
            return null;
        }
    }

    private BufferedImage invertImageColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = image.getRGB(x, y);
                Color col = new Color(rgba, true);
                // Invertimos R, G y B
                int r = 255 - col.getRed();
                int g = 255 - col.getGreen();
                int b = 255 - col.getBlue();
                // Mantenemos el Alpha (transparencia) intacto
                Color negativeColor = new Color(r, g, b, col.getAlpha());
                result.setRGB(x, y, negativeColor.getRGB());
            }
        }
        return result;
    }

    private void updateIcons() {
        // Actualizar íconos de la barra de herramientas (tamaño 24)
        if (btnToolbarLoad != null) btnToolbarLoad.setIcon(loadIcon("/assets/icons/add.png", 24));
        if (btnToolbarSave != null) btnToolbarSave.setIcon(loadIcon("/assets/icons/save.png", 24));
        if (btnToolbarTheme != null) btnToolbarTheme.setIcon(loadIcon("/assets/icons/theme.png", 24));
        
        // Ícono del botón de limpiar filtros (tamaño 20)
        if (btnSidebarReset != null) btnSidebarReset.setIcon(loadIcon("/assets/icons/clear-filter.png", 20));

        // Actualizar íconos del menú (tamaño 16)
        if (itemLoad != null) itemLoad.setIcon(loadIcon("/assets/icons/add.png", 16));
        if (itemSave != null) itemSave.setIcon(loadIcon("/assets/icons/save.png", 16));
        if (itemTheme != null) itemTheme.setIcon(loadIcon("/assets/icons/theme.png", 16));
    }

    // --- MÉTODOS DE ACCIÓN ---

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(chooser.getSelectedFile());
                currentFilteredImage = originalImage; // Inicialmente son iguales
                updateImagesDisplay();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al leer imagen.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveImage() {
        if (currentFilteredImage == null) {
            JOptionPane.showMessageDialog(this, "No hay imagen para guardar.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar Imagen Filtrada");
        // Opción por defecto
        chooser.setSelectedFile(new File("imagen_filtrada.png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            // Aseguramos que tenga extensión .png
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".png");
            }

            try {
                // Guardamos la imagen modificada en disco
                ImageIO.write(currentFilteredImage, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "Imagen guardada con éxito en:\n" + fileToSave.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
            }
        }
    }

    private void applyFilter(ImageFilter filter) {
        if (originalImage != null) {
            // CRÍTICO: Siempre aplicamos el filtro sobre la ORIGINAL
            // Esto evita que los filtros se apilen (No más negativo del negativo)
            currentFilteredImage = filter.apply(originalImage);
            filteredImageLabel.setIcon(new ImageIcon(currentFilteredImage));
            filteredImageLabel.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Primero carga una imagen desde el menú Archivo o la barra de herramientas.");
        }
    }

    private void resetImage() {
        if (originalImage != null) {
            currentFilteredImage = originalImage;
            filteredImageLabel.setIcon(new ImageIcon(originalImage));
            filteredImageLabel.setText("");
        }
    }

    private void updateImagesDisplay() {
        if (originalImage != null) {
            originalImageLabel.setText("");
            filteredImageLabel.setText("");
            originalImageLabel.setIcon(new ImageIcon(originalImage));
            filteredImageLabel.setIcon(new ImageIcon(currentFilteredImage));
        }
    }

    // --- MAGIA DE FLATLAF: CAMBIO DE TEMA EN TIEMPO REAL ---
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        updateIcons(); // Actualizar los íconos a blanco/negro según corresponda

        try {
            // Inicia una animación para que el cambio no sea brusco
            FlatAnimatedLafChange.showSnapshot();

            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            // Actualiza toda la interfaz
            FlatLaf.updateUI();

            // Oculta la animación suavemente
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Exception ex) {
            System.err.println("Error al cambiar de tema.");
        }
    }
}