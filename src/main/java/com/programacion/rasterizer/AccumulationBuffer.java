package com.programacion.rasterizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Buffer de Acumulación (Accumulation Buffer) para el procesamiento y mezcla
 * de múltiples fotogramas con precisión de punto flotante. Evita la pérdida
 * de información cromática durante acumulaciones repetidas (Capítulo 10).
 */
public class AccumulationBuffer {
    private int width;
    private int height;
    private float[] accumR;
    private float[] accumG;
    private float[] accumB;
    private float[] accumA;

    /**
     * Crea un buffer de acumulación vacío. Debe inicializarse con resize().
     */
    public AccumulationBuffer() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Crea un buffer de acumulación con dimensiones fijas.
     *
     * @param width  Ancho del buffer.
     * @param height Alto del buffer.
     */
    public AccumulationBuffer(int width, int height) {
        resize(width, height);
    }

    /**
     * Ajusta el tamaño de los buffers flotantes de acumulación.
     * Si las dimensiones coinciden y el buffer ya existe, conserva el contenido actual.
     *
     * @param w Nuevo ancho.
     * @param h Nuevo alto.
     */
    public void resize(int w, int h) {
        if (this.width == w && this.height == h && accumR != null) {
            return;
        }
        this.width = w;
        this.height = h;
        int size = w * h;
        this.accumR = new float[size];
        this.accumG = new float[size];
        this.accumB = new float[size];
        this.accumA = new float[size];
        clear();
    }

    /**
     * Inicializa a cero todos los canales de acumulación (R, G, B, A).
     */
    public void clear() {
        if (accumR != null) {
            Arrays.fill(accumR, 0.0f);
            Arrays.fill(accumG, 0.0f);
            Arrays.fill(accumB, 0.0f);
            Arrays.fill(accumA, 0.0f);
        }
    }

    /**
     * Acumula una matriz de píxeles ARGB de entrada aplicando un peso lineal.
     *
     * @param pixels Arreglo lineal de píxeles ARGB.
     * @param weight Factor de peso para la mezcla lineal.
     */
    public void accumulate(int[] pixels, float weight) {
        if (pixels == null || accumR == null) return;
        int size = Math.min(pixels.length, width * height);
        for (int i = 0; i < size; i++) {
            int argb = pixels[i];
            float a = (argb >> 24) & 0xFF;
            float r = (argb >> 16) & 0xFF;
            float g = (argb >> 8) & 0xFF;
            float b = argb & 0xFF;

            accumR[i] += r * weight;
            accumG[i] += g * weight;
            accumB[i] += b * weight;
            accumA[i] += a * weight;
        }
    }

    /**
     * Acumula los píxeles de una BufferedImage con un factor de peso.
     *
     * @param img    Imagen de entrada.
     * @param weight Factor de peso.
     */
    public void accumulate(BufferedImage img, float weight) {
        if (img == null) return;
        resize(img.getWidth(), img.getHeight());
        int[] pixels;
        if (img.getType() == BufferedImage.TYPE_INT_ARGB && img.getRaster().getDataBuffer() instanceof DataBufferInt) {
            pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        } else {
            pixels = img.getRGB(0, 0, width, height, null, 0, width);
        }
        accumulate(pixels, weight);
    }

    /**
     * Multiplica todos los valores acumulados por un factor de escala.
     *
     * @param scale Escala de atenuación.
     */
    public void scaleAccumulation(float scale) {
        if (accumR == null) return;
        int size = width * height;
        for (int i = 0; i < size; i++) {
            accumR[i] *= scale;
            accumG[i] *= scale;
            accumB[i] *= scale;
            accumA[i] *= scale;
        }
    }

    /**
     * Retorna y convierte la acumulación flotante a un arreglo ARGB de 32 bits destino,
     * aplicando un factor de escala y limitando los valores a [0, 255].
     *
     * @param destPixels Buffer ARGB de destino.
     * @param scale      Factor de escala global (ej. 1.0f para retorno directo, 1/N si no se pre-ponderó).
     */
    public void returnFrame(int[] destPixels, float scale) {
        if (destPixels == null || accumR == null) return;
        int size = Math.min(destPixels.length, width * height);
        for (int i = 0; i < size; i++) {
            int r = Math.max(0, Math.min(255, Math.round(accumR[i] * scale)));
            int g = Math.max(0, Math.min(255, Math.round(accumG[i] * scale)));
            int b = Math.max(0, Math.min(255, Math.round(accumB[i] * scale)));
            int a = Math.max(0, Math.min(255, Math.round(accumA[i] * scale)));

            destPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Retorna la acumulación a una BufferedImage.
     *
     * @param img   Imagen de destino.
     * @param scale Factor de escala.
     */
    public void returnFrame(BufferedImage img, float scale) {
        if (img == null) return;
        int w = img.getWidth();
        int h = img.getHeight();
        resize(w, h);
        int[] pixels;
        if (img.getType() == BufferedImage.TYPE_INT_ARGB && img.getRaster().getDataBuffer() instanceof DataBufferInt) {
            pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            returnFrame(pixels, scale);
        } else {
            pixels = new int[w * h];
            returnFrame(pixels, scale);
            img.setRGB(0, 0, w, h, pixels, 0, w);
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float[] getAccumR() { return accumR; }
    public float[] getAccumG() { return accumG; }
    public float[] getAccumB() { return accumB; }
    public float[] getAccumA() { return accumA; }
}
