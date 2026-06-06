package com.programacion.rasterizer;

/**
 * Representa un fragmento (DTO) generado durante el proceso de rasterización.
 * Contiene coordenadas de pantalla, profundidad y color ARGB.
 */
public class Fragment {
    public int x;
    public int y;
    public double z;
    public double u;
    public double v;
    public double w;
    public int color;
    public boolean discarded = false;
    public double coverage = 1.0;

    public Fragment(int x, int y, double z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public Fragment(int x, int y, double z, double u, double v, double w, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
        this.w = w;
        this.color = color;
    }
}
