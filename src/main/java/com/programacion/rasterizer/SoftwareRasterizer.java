package com.programacion.rasterizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Motor nuclear de rasterización por software (SoftwareRasterizer).
 * Controla directamente arreglos unidimensionales de píxeles y realiza pruebas de visibilidad
 * mediante un buffer de profundidad (Z-Buffer).
 */
public class SoftwareRasterizer {
    private int width;
    private int height;
    private int[] pixels;
    private double[] zBuffer;
    private double[] wBuffer;
    private FragmentConsumer consumer;
    private boolean zBufferEnabled = true;
    private boolean wBufferEnabled = true;

    /**
     * Crea un rasterizador por software vacío. Debe inicializarse vinculando una imagen
     * con bindTarget().
     */
    public SoftwareRasterizer() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Crea un rasterizador independiente con dimensiones fijas.
     *
     * @param width Ancho del buffer.
     * @param height Alto del buffer.
     */
    public SoftwareRasterizer(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
        this.zBuffer = new double[width * height];
        clearZBuffer();
    }

    /**
     * Vincula directamente el buffer de píxeles de una BufferedImage de tipo TYPE_INT_ARGB
     * para manipulación ultra-eficiente en memoria.
     *
     * @param image Imagen objetivo de tipo TYPE_INT_ARGB.
     */
    public void bindTarget(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("La imagen objetivo no puede ser nula.");
        }
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException("La imagen debe ser de tipo TYPE_INT_ARGB para acceso directo.");
        }
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        if (this.zBuffer == null || this.zBuffer.length != width * height) {
            this.zBuffer = new double[width * height];
        }
        if (this.wBuffer == null || this.wBuffer.length != width * height) {
            this.wBuffer = new double[width * height];
        }
    }

    /**
     * Limpia el buffer de color (píxeles) con un color ARGB específico.
     */
    public void clearColorBuffer(int argbColor) {
        if (pixels != null) {
            Arrays.fill(pixels, argbColor);
        }
    }

    /**
     * Inicializa o limpia el buffer de profundidad (Z-Buffer) a su valor máximo.
     */
    public void clearZBuffer() {
        if (zBuffer != null) {
            Arrays.fill(zBuffer, Double.MAX_VALUE);
        }
        if (wBuffer != null) {
            Arrays.fill(wBuffer, Double.MAX_VALUE);
        }
    }

    /**
     * Configura el consumidor de fragmentos para acoplamiento con otros pipelines.
     */
    public void setFragmentConsumer(FragmentConsumer consumer) {
        this.consumer = consumer;
    }

    public FragmentConsumer getFragmentConsumer() {
        return this.consumer;
    }

    /**
     * Activa o desactiva la prueba de profundidad (Z-Buffer).
     */
    public void setZBufferEnabled(boolean enabled) {
        this.zBufferEnabled = enabled;
    }

    public boolean isZBufferEnabled() {
        return zBufferEnabled;
    }

    public void setWBufferEnabled(boolean enabled) {
        this.wBufferEnabled = enabled;
    }

    public boolean isWBufferEnabled() {
        return wBufferEnabled;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPixels() {
        return pixels;
    }

    public double[] getZBuffer() {
        return zBuffer;
    }

    public double[] getWBuffer() {
        return wBuffer;
    }

    private boolean isMsaaEnabled() {
        return (consumer instanceof FragmentPipeline) && ((FragmentPipeline) consumer).isMsaaEnabled();
    }

    /**
     * Compara y escribe un fragmento en el framebuffer realizando la prueba de profundidad.
     */
    private void writeFragment(int x, int y, double z, int color) {
        writeFragment(x, y, z, 0.0, 0.0, z, color);
    }

    /**
     * Compara y escribe un fragmento en el framebuffer realizando la prueba de profundidad (Z-Buffer o W-Buffer).
     */
    private void writeFragment(int x, int y, double z, double u, double v, double wVal, int color) {
        Fragment frag = new Fragment(x, y, z, u, v, wVal, color);
        writeFragment(frag);
    }

    /**
     * Procesa y escribe un objeto Fragment en el framebuffer respetando el pipeline.
     */
    private void writeFragment(Fragment fragment) {
        int x = fragment.x;
        int y = fragment.y;
        if (x < 0 || x >= width || y < 0 || y >= height) return;

        // Si hay un pipeline externo configurado (Capítulo 8), emitimos el fragmento.
        if (consumer != null) {
            consumer.consume(fragment);
            if (consumer instanceof FragmentPipeline) {
                return;
            }
        }

        // Si el fragmento es descartado por el pipeline, detenemos el proceso de escritura.
        if (fragment.discarded) {
            return;
        }

        // Validar si las coordenadas siguen vigentes tras el procesamiento externo
        int fx = fragment.x;
        int fy = fragment.y;
        if (fx < 0 || fx >= width || fy < 0 || fy >= height) return;

        int index = fy * width + fx;

        // Prueba de profundidad (Z-Buffer o W-Buffer)
        if (wBufferEnabled) {
            if (!zBufferEnabled || fragment.w < wBuffer[index]) {
                if (zBufferEnabled) {
                    wBuffer[index] = fragment.w;
                }
                pixels[index] = fragment.color;
            }
        } else {
            if (!zBufferEnabled || fragment.z < zBuffer[index]) {
                if (zBufferEnabled) {
                    zBuffer[index] = fragment.z;
                }
                pixels[index] = fragment.color;
            }
        }
    }

    /**
     * Dibuja un punto (Píxel individual) en las coordenadas y profundidad indicadas.
     */
    public void drawPoint(int x, int y, double z, int color) {
        writeFragment(x, y, z, color);
    }

    /**
     * Dibuja una línea en 3D utilizando el algoritmo de Bresenham, interpolando la profundidad Z linealmente.
     */
    public void drawLineBresenham(int x1, int y1, double z1, int x2, int y2, double z2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        int err = dx - dy;

        int x = x1;
        int y = y1;
        int steps = Math.max(dx, dy);
        int step = 0;

        while (true) {
            double t = steps == 0 ? 1.0 : (double) step / steps;
            double z = z1 + t * (z2 - z1);
            writeFragment(x, y, z, color);

            if (x == x2 && y == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
            step++;
        }
    }

    /**
     * Dibuja una línea en 3D utilizando el algoritmo DDA (Digital Differential Analyzer),
     * interpolando la profundidad Z linealmente.
     */
    public void drawLineDDA(int x1, int y1, double z1, int x2, int y2, double z2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double steps = Math.max(Math.abs(dx), Math.abs(dy));

        if (steps == 0) {
            writeFragment(x1, y1, z1, color);
            return;
        }

        double xInc = dx / steps;
        double yInc = dy / steps;
        double zInc = dz / steps;

        double x = x1;
        double y = y1;
        double z = z1;

        for (int i = 0; i <= steps; i++) {
            writeFragment((int) Math.round(x), (int) Math.round(y), z, color);
            x += xInc;
            y += yInc;
            z += zInc;
        }
    }

    /**
     * Dibuja un triángulo sólido con un color uniforme.
     */
    public void drawTriangle(int x1, int y1, double z1,
                             int x2, int y2, double z2,
                             int x3, int y3, double z3,
                             int color) {
        drawTriangle(x1, y1, z1, x2, y2, z2, x3, y3, z3, color, color, color);
    }

    /**
     * Rasteriza un triángulo en 3D utilizando coordenadas baricéntricas.
     * Soporta interpolación de profundidad Z e interpolación de color en los vértices (Gouraud shading).
     */
    public void drawTriangle(int x1, int y1, double z1,
                             int x2, int y2, double z2,
                             int x3, int y3, double z3,
                             int c1, int c2, int c3) {
        // Encontrar la caja delimitadora (Bounding Box) del triángulo
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        // Recortar contra las dimensiones de pantalla (Clipping bidimensional)
        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        if (minX > maxX || minY > maxY) return;

        // Denominador para coordenadas baricéntricas
        double denom = (double) ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (Math.abs(denom) < 1e-9) {
            // El triángulo es colineal o degenerado, dibujamos sus aristas como líneas
            drawLineBresenham(x1, y1, z1, x2, y2, z2, c1);
            drawLineBresenham(x2, y2, z2, x3, y3, z3, c2);
            drawLineBresenham(x3, y3, z3, x1, y1, z1, c3);
            return;
        }

        double invDenom = 1.0 / denom;

        // Iterar en la caja delimitadora
        boolean msaa = isMsaaEnabled();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double coverage = 1.0;
                boolean renderPixel = false;

                if (msaa) {
                    int coveredCount = 0;
                    double[] subOffsets = { 0.25, 0.75 };
                    for (double dy : subOffsets) {
                        for (double dx : subOffsets) {
                            double px = x + dx;
                            double py = y + dy;
                            double sw1 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) * invDenom;
                            double sw2 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) * invDenom;
                            double sw3 = 1.0 - sw1 - sw2;
                            if (sw1 >= -1e-9 && sw2 >= -1e-9 && sw3 >= -1e-9) {
                                coveredCount++;
                            }
                        }
                    }
                    if (coveredCount > 0) {
                        renderPixel = true;
                        coverage = coveredCount / 4.0;
                    }
                } else {
                    double w1 = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) * invDenom;
                    double w2 = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) * invDenom;
                    double w3 = 1.0 - w1 - w2;
                    if (w1 >= -1e-9 && w2 >= -1e-9 && w3 >= -1e-9) {
                        renderPixel = true;
                    }
                }

                // Si está dentro (permitiendo un margen infinitesimal para precisión numérica)
                if (renderPixel) {
                    // Calcular pesos baricéntricos en el centro del píxel para interpolación
                    double w1 = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) * invDenom;
                    double w2 = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) * invDenom;
                    double w3 = 1.0 - w1 - w2;

                    // Interpolación lineal de Z
                    double z = w1 * z1 + w2 * z2 + w3 * z3;

                    // Interpolación lineal de componentes de color (ARGB)
                    int a = (int) (w1 * ((c1 >> 24) & 0xFF) + w2 * ((c2 >> 24) & 0xFF) + w3 * ((c3 >> 24) & 0xFF));
                    int r = (int) (w1 * ((c1 >> 16) & 0xFF) + w2 * ((c2 >> 16) & 0xFF) + w3 * ((c3 >> 16) & 0xFF));
                    int g = (int) (w1 * ((c1 >> 8) & 0xFF) + w2 * ((c2 >> 8) & 0xFF) + w3 * ((c3 >> 8) & 0xFF));
                    int b = (int) (w1 * (c1 & 0xFF) + w2 * (c2 & 0xFF) + w3 * (c3 & 0xFF));

                    // Clampear componentes
                    a = Math.max(0, Math.min(255, a));
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));

                    int interpolatedColor = (a << 24) | (r << 16) | (g << 8) | b;
                    Fragment frag = new Fragment(x, y, z, interpolatedColor);
                    frag.coverage = coverage;
                    writeFragment(frag);
                }
            }
        }
    }

    /**
     * Dibuja los valores del Z-buffer o W-buffer representados como escala de grises en el buffer de píxeles principal.
     * Útil para inspección visual y demostraciones didácticas.
     */
    public void drawZBufferToColorBuffer() {
        if (pixels == null) return;
        double[] bufferToDraw = wBufferEnabled ? wBuffer : zBuffer;
        if (bufferToDraw == null) return;

        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;

        // Encontrar los límites de profundidad vigentes
        for (double val : bufferToDraw) {
            if (val != Double.MAX_VALUE) {
                if (val < minVal) minVal = val;
                if (val > maxVal) maxVal = val;
            }
        }

        // Si el buffer de profundidad está completamente libre de contenido
        if (minVal == Double.MAX_VALUE) {
            Arrays.fill(pixels, 0xFF000000); // Rellenar con negro completo
            return;
        }

        double range = maxVal - minVal;
        if (range == 0) range = 1.0;

        for (int i = 0; i < pixels.length; i++) {
            double val = bufferToDraw[i];
            if (val == Double.MAX_VALUE) {
                pixels[i] = 0xFF000000; // Píxeles sin profundidad pintados de negro
            } else {
                // Mapear el valor de manera inversa para que lo cercano (menor valor)
                // se muestre con blanco/brillante
                double norm = 1.0 - ((val - minVal) / range);
                int pixelIntensity = (int) (norm * 255.0);
                pixelIntensity = Math.max(0, Math.min(255, pixelIntensity));
                pixels[i] = 0xFF000000 | (pixelIntensity << 16) | (pixelIntensity << 8) | pixelIntensity;
            }
        }
    }

    /**
     * Muestrea una textura usando interpolación bilineal y envoltura (wrapping) de coordenadas.
     */
    private int sampleTextureBilinear(int[] texPixels, int texW, int texH, double u, double v) {
        // Envoltura de textura (wrapping a rango [0.0, 1.0])
        double uw = u - Math.floor(u);
        double vw = v - Math.floor(v);

        double tx = uw * (texW - 1);
        double ty = vw * (texH - 1);

        int x0 = (int) tx;
        int y0 = (int) ty;
        int x1 = Math.min(texW - 1, x0 + 1);
        int y1 = Math.min(texH - 1, y0 + 1);

        double kx = tx - x0;
        double ky = ty - y0;

        int p00 = texPixels[y0 * texW + x0];
        int p10 = texPixels[y0 * texW + x1];
        int p01 = texPixels[y1 * texW + x0];
        int p11 = texPixels[y1 * texW + x1];

        int a00 = (p00 >> 24) & 0xFF;
        int r00 = (p00 >> 16) & 0xFF;
        int g00 = (p00 >> 8) & 0xFF;
        int b00 = p00 & 0xFF;

        int a10 = (p10 >> 24) & 0xFF;
        int r10 = (p10 >> 16) & 0xFF;
        int g10 = (p10 >> 8) & 0xFF;
        int b10 = p10 & 0xFF;

        int a01 = (p01 >> 24) & 0xFF;
        int r01 = (p01 >> 16) & 0xFF;
        int g01 = (p01 >> 8) & 0xFF;
        int b01 = p01 & 0xFF;

        int a11 = (p11 >> 24) & 0xFF;
        int r11 = (p11 >> 16) & 0xFF;
        int g11 = (p11 >> 8) & 0xFF;
        int b11 = p11 & 0xFF;

        double a0 = a00 + kx * (a10 - a00);
        double r0 = r00 + kx * (r10 - r00);
        double g0 = g00 + kx * (g10 - g00);
        double b0 = b00 + kx * (b10 - b00);

        double a1 = a01 + kx * (a11 - a01);
        double r1 = r01 + kx * (r11 - r01);
        double g1 = g01 + kx * (g11 - g01);
        double b1 = b01 + kx * (b11 - b01);

        int a = (int) (a0 + ky * (a1 - a0));
        int r = (int) (r0 + ky * (r1 - r0));
        int g = (int) (g0 + ky * (g1 - g0));
        int b = (int) (b0 + ky * (b1 - b0));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Multiplica dos colores ARGB component-wise.
     */
    private int multiplyColors(int c1, int c2) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (a1 * a2) / 255;
        int r = (r1 * r2) / 255;
        int g = (g1 * g2) / 255;
        int b = (b1 * b2) / 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Rasteriza un triángulo texturizado con soporte de W-Buffer e interpolación corregida por perspectiva (1/W).
     */
    public void drawTriangleTextured(int x1, int y1, double z1, double w1, double u1, double v1,
                                     int x2, int y2, double z2, double w2, double u2, double v2,
                                     int x3, int y3, double z3, double w3, double u3, double v3,
                                     BufferedImage texture,
                                     int c1, int c2, int c3) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        minX = Math.max(0, minX);
        maxX = Math.min(width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(height - 1, maxY);

        if (minX > maxX || minY > maxY) return;

        double denom = (double) ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (Math.abs(denom) < 1e-9) {
            drawLineBresenham(x1, y1, z1, x2, y2, z2, c1);
            drawLineBresenham(x2, y2, z2, x3, y3, z3, c2);
            drawLineBresenham(x3, y3, z3, x1, y1, z1, c3);
            return;
        }

        double invDenom = 1.0 / denom;

        int texW = texture.getWidth();
        int texH = texture.getHeight();
        int[] texPixels;
        if (texture.getType() == BufferedImage.TYPE_INT_ARGB && texture.getRaster().getDataBuffer() instanceof DataBufferInt) {
            texPixels = ((DataBufferInt) texture.getRaster().getDataBuffer()).getData();
        } else {
            BufferedImage converted = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics g = converted.getGraphics();
            g.drawImage(texture, 0, 0, null);
            g.dispose();
            texPixels = ((DataBufferInt) converted.getRaster().getDataBuffer()).getData();
        }

        // Atributos divididos por W para interpolación en perspectiva
        double invW1 = 1.0 / w1;
        double invW2 = 1.0 / w2;
        double invW3 = 1.0 / w3;

        double uOverW1 = u1 * invW1;
        double uOverW2 = u2 * invW2;
        double uOverW3 = u3 * invW3;

        double vOverW1 = v1 * invW1;
        double vOverW2 = v2 * invW2;
        double vOverW3 = v3 * invW3;

        double rOverW1 = ((c1 >> 16) & 0xFF) * invW1;
        double gOverW1 = ((c1 >> 8) & 0xFF) * invW1;
        double bOverW1 = (c1 & 0xFF) * invW1;
        double aOverW1 = ((c1 >> 24) & 0xFF) * invW1;

        double rOverW2 = ((c2 >> 16) & 0xFF) * invW2;
        double gOverW2 = ((c2 >> 8) & 0xFF) * invW2;
        double bOverW2 = (c2 & 0xFF) * invW2;
        double aOverW2 = ((c2 >> 24) & 0xFF) * invW2;

        double rOverW3 = ((c3 >> 16) & 0xFF) * invW3;
        double gOverW3 = ((c3 >> 8) & 0xFF) * invW3;
        double bOverW3 = (c3 & 0xFF) * invW3;
        double aOverW3 = ((c3 >> 24) & 0xFF) * invW3;

        boolean msaa = isMsaaEnabled();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double coverage = 1.0;
                boolean renderPixel = false;

                if (msaa) {
                    int coveredCount = 0;
                    double[] subOffsets = { 0.25, 0.75 };
                    for (double dy : subOffsets) {
                        for (double dx : subOffsets) {
                            double px = x + dx;
                            double py = y + dy;
                            double sw1 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) * invDenom;
                            double sw2 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) * invDenom;
                            double sw3 = 1.0 - sw1 - sw2;
                            if (sw1 >= -1e-9 && sw2 >= -1e-9 && sw3 >= -1e-9) {
                                coveredCount++;
                            }
                        }
                    }
                    if (coveredCount > 0) {
                        renderPixel = true;
                        coverage = coveredCount / 4.0;
                    }
                } else {
                    double w1_bary = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) * invDenom;
                    double w2_bary = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) * invDenom;
                    double w3_bary = 1.0 - w1_bary - w2_bary;
                    if (w1_bary >= -1e-9 && w2_bary >= -1e-9 && w3_bary >= -1e-9) {
                        renderPixel = true;
                    }
                }

                if (renderPixel) {
                    double w1_bary = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) * invDenom;
                    double w2_bary = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) * invDenom;
                    double w3_bary = 1.0 - w1_bary - w2_bary;

                    double z = w1_bary * z1 + w2_bary * z2 + w3_bary * z3;
                    int finalColor;
                    double u, v, wPixel;

                    if (wBufferEnabled) {
                        double oneOverW = w1_bary * invW1 + w2_bary * invW2 + w3_bary * invW3;
                        wPixel = 1.0 / oneOverW;

                        u = (w1_bary * uOverW1 + w2_bary * uOverW2 + w3_bary * uOverW3) * wPixel;
                        v = (w1_bary * vOverW1 + w2_bary * vOverW2 + w3_bary * vOverW3) * wPixel;

                        int r = (int) ((w1_bary * rOverW1 + w2_bary * rOverW2 + w3_bary * rOverW3) * wPixel);
                        int g = (int) ((w1_bary * gOverW1 + w2_bary * gOverW2 + w3_bary * gOverW3) * wPixel);
                        int b = (int) ((w1_bary * bOverW1 + w2_bary * bOverW2 + w3_bary * bOverW3) * wPixel);
                        int a = (int) ((w1_bary * aOverW1 + w2_bary * aOverW2 + w3_bary * aOverW3) * wPixel);

                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));
                        a = Math.max(0, Math.min(255, a));

                        int gouraudColor = (a << 24) | (r << 16) | (g << 8) | b;
                        int texColor = sampleTextureBilinear(texPixels, texW, texH, u, v);
                        finalColor = multiplyColors(texColor, gouraudColor);
                    } else {
                        u = w1_bary * u1 + w2_bary * u2 + w3_bary * u3;
                        v = w1_bary * v1 + w2_bary * v2 + w3_bary * v3;
                        wPixel = w1_bary * w1 + w2_bary * w2 + w3_bary * w3;

                        int r = (int) (w1_bary * ((c1 >> 16) & 0xFF) + w2_bary * ((c2 >> 16) & 0xFF) + w3_bary * ((c3 >> 16) & 0xFF));
                        int g = (int) (w1_bary * ((c1 >> 8) & 0xFF) + w2_bary * ((c2 >> 8) & 0xFF) + w3_bary * ((c3 >> 8) & 0xFF));
                        int b = (int) (w1_bary * (c1 & 0xFF) + w2_bary * (c2 & 0xFF) + w3_bary * (c3 & 0xFF));
                        int a = (int) (w1_bary * ((c1 >> 24) & 0xFF) + w2_bary * ((c2 >> 24) & 0xFF) + w3_bary * ((c3 >> 24) & 0xFF));

                        r = Math.max(0, Math.min(255, r));
                        g = Math.max(0, Math.min(255, g));
                        b = Math.max(0, Math.min(255, b));
                        a = Math.max(0, Math.min(255, a));

                        int gouraudColor = (a << 24) | (r << 16) | (g << 8) | b;
                        int texColor = sampleTextureBilinear(texPixels, texW, texH, u, v);
                        finalColor = multiplyColors(texColor, gouraudColor);
                    }

                    Fragment frag = new Fragment(x, y, z, u, v, wPixel, finalColor);
                    frag.coverage = coverage;
                    writeFragment(frag);
                }
            }
        }
    }
}
