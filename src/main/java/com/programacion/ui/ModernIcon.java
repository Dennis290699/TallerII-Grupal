package com.programacion.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class ModernIcon implements Icon {
    private final String type;
    private final int size;
    private final Color color;

    public ModernIcon(String type, int size) {
        this(type, size, null);
    }

    public ModernIcon(String type, int size, Color color) {
        this.type = type.toLowerCase();
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color paintColor = color;
        if (paintColor == null) {
            paintColor = c != null ? c.getForeground() : Color.WHITE;
        }
        g2d.setColor(paintColor);

        // Ajustar el grosor de línea según el tamaño del icono
        float strokeWidth = Math.max(1.5f, size / 12.0f);
        g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Dimensiones útiles del icono con margen
        double margin = size * 0.15;
        double w = size - (2 * margin);
        double h = size - (2 * margin);
        double left = x + margin;
        double top = y + margin;

        switch (type) {
            case "cargar": // Icono de Carpeta con Flecha o Más
                Path2D folder = new Path2D.Double();
                folder.moveTo(left, top + h * 0.8);
                folder.lineTo(left, top + h * 0.1);
                folder.lineTo(left + w * 0.35, top + h * 0.1);
                folder.lineTo(left + w * 0.5, top + h * 0.25);
                folder.lineTo(left + w * 0.9, top + h * 0.25);
                folder.lineTo(left + w * 0.9, top + h * 0.8);
                folder.closePath();
                g2d.draw(folder);
                // Dibujar un pequeño '+' en el centro
                g2d.setStroke(new BasicStroke(strokeWidth * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(new Line2D.Double(left + w * 0.35, top + h * 0.55, left + w * 0.65, top + h * 0.55));
                g2d.draw(new Line2D.Double(left + w * 0.5, top + h * 0.4, left + w * 0.5, top + h * 0.7));
                break;

            case "guardar": // Icono de Disquete
                Path2D disk = new Path2D.Double();
                disk.moveTo(left, top + h);
                disk.lineTo(left, top);
                disk.lineTo(left + w * 0.75, top);
                disk.lineTo(left + w, top + h * 0.25);
                disk.lineTo(left + w, top + h);
                disk.closePath();
                g2d.draw(disk);
                // Rectángulo central
                g2d.draw(new Rectangle2D.Double(left + w * 0.25, top + h * 0.5, w * 0.5, h * 0.5));
                // Línea deslizador superior
                g2d.draw(new Line2D.Double(left + w * 0.3, top + h * 0.2, left + w * 0.6, top + h * 0.2));
                break;

            case "limpiar": // Icono de Basura
                g2d.draw(new Rectangle2D.Double(left + w * 0.15, top + h * 0.25, w * 0.7, h * 0.75));
                g2d.draw(new Line2D.Double(left, top + h * 0.25, left + w, top + h * 0.25));
                g2d.draw(new Line2D.Double(left + w * 0.3, top + h * 0.1, left + w * 0.7, top + h * 0.1));
                g2d.draw(new Line2D.Double(left + w * 0.35, top + h * 0.4, left + w * 0.35, top + h * 0.85));
                g2d.draw(new Line2D.Double(left + w * 0.5, top + h * 0.4, left + w * 0.5, top + h * 0.85));
                g2d.draw(new Line2D.Double(left + w * 0.65, top + h * 0.4, left + w * 0.65, top + h * 0.85));
                break;

            case "ver": // Icono de Ojo
                Path2D eye = new Path2D.Double();
                eye.moveTo(left, top + h * 0.5);
                eye.quadTo(left + w * 0.5, top - h * 0.1, left + w, top + h * 0.5);
                eye.quadTo(left + w * 0.5, top + h * 1.1, left, top + h * 0.5);
                eye.closePath();
                g2d.draw(eye);
                // Pupila
                g2d.fill(new Ellipse2D.Double(left + w * 0.35, top + h * 0.35, w * 0.3, h * 0.3));
                break;

            case "tema": // Sol / Luna
                // Para simplificar, una luna creciente estilizada (muy estética en modo oscuro)
                // y que combina bien en ambos
                Path2D moon = new Path2D.Double();
                moon.moveTo(left + w * 0.6, top);
                moon.quadTo(left, top + h * 0.1, left + w * 0.1, top + h * 0.8);
                moon.quadTo(left + w * 0.8, top + h * 1.1, left + w, top + h * 0.4);
                moon.quadTo(left + w * 0.4, top + h * 0.5, left + w * 0.6, top);
                moon.closePath();
                g2d.fill(moon);
                break;

            case "avanzado": // Capas apiladas (Modo Apilado)
                Path2D lay1 = new Path2D.Double();
                lay1.moveTo(left + w * 0.5, top);
                lay1.lineTo(left + w, top + h * 0.25);
                lay1.lineTo(left + w * 0.5, top + h * 0.5);
                lay1.lineTo(left, top + h * 0.25);
                lay1.closePath();
                g2d.draw(lay1);

                // Capa media inferior
                Path2D lay2 = new Path2D.Double();
                lay2.moveTo(left, top + h * 0.5);
                lay2.lineTo(left + w * 0.5, top + h * 0.75);
                lay2.lineTo(left + w, top + h * 0.5);
                g2d.draw(lay2);

                // Capa base inferior
                Path2D lay3 = new Path2D.Double();
                lay3.moveTo(left, top + h * 0.75);
                lay3.lineTo(left + w * 0.5, top + h);
                lay3.lineTo(left + w, top + h * 0.75);
                g2d.draw(lay3);
                break;

            case "blending": // Blending / Mezclador (Dos círculos intersecados)
                g2d.draw(new Ellipse2D.Double(left, top + h * 0.1, w * 0.65, h * 0.65));
                g2d.draw(new Ellipse2D.Double(left + w * 0.35, top + h * 0.25, w * 0.65, h * 0.65));
                break;

            case "deshacer": // Deshacer (Flecha curva izquierda)
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(left + w * 0.8, top + h * 0.8);
                arrow.quadTo(left + w * 0.8, top + h * 0.2, left + w * 0.3, top + h * 0.3);
                g2d.draw(arrow);
                // Punta de la flecha
                Path2D head = new Path2D.Double();
                head.moveTo(left + w * 0.45, top + h * 0.1);
                head.lineTo(left + w * 0.2, top + h * 0.3);
                head.lineTo(left + w * 0.45, top + h * 0.5);
                g2d.draw(head);
                break;

            case "reiniciar": // Reiniciar todo (Flecha circular)
                g2d.draw(new Arc2D.Double(left, top, w, h, 45, 270, Arc2D.OPEN));
                // Punta de flecha en el extremo superior
                Path2D rHead = new Path2D.Double();
                rHead.moveTo(left + w * 0.5, top - h * 0.1);
                rHead.lineTo(left + w * 0.8, top + h * 0.15);
                rHead.lineTo(left + w * 0.5, top + h * 0.35);
                g2d.draw(rHead);
                break;

            case "volver": // Volver al editor simple (Flecha recta a la izquierda)
                g2d.draw(new Line2D.Double(left + w, top + h * 0.5, left, top + h * 0.5));
                Path2D vHead = new Path2D.Double();
                vHead.moveTo(left + w * 0.35, top + h * 0.2);
                vHead.lineTo(left, top + h * 0.5);
                vHead.lineTo(left + w * 0.35, top + h * 0.8);
                g2d.draw(vHead);
                break;

            case "subir": // Chevron arriba
                Path2D cup = new Path2D.Double();
                cup.moveTo(left, top + h * 0.7);
                cup.lineTo(left + w * 0.5, top + h * 0.3);
                cup.lineTo(left + w, top + h * 0.7);
                g2d.draw(cup);
                break;

            case "bajar": // Chevron abajo
                Path2D cdn = new Path2D.Double();
                cdn.moveTo(left, top + h * 0.3);
                cdn.lineTo(left + w * 0.5, top + h * 0.7);
                cdn.lineTo(left + w, top + h * 0.3);
                g2d.draw(cdn);
                break;

            case "agregar": // Signo más
                g2d.draw(new Line2D.Double(left, top + h * 0.5, left + w, top + h * 0.5));
                g2d.draw(new Line2D.Double(left + w * 0.5, top, left + w * 0.5, top + h));
                break;

            case "eliminar": // Signo menos / Papelera simple
                g2d.draw(new Line2D.Double(left, top + h * 0.5, left + w, top + h * 0.5));
                break;

            case "histograma": // Histograma mini
                g2d.draw(new Line2D.Double(left, top + h, left + w, top + h));
                g2d.draw(new Line2D.Double(left, top, left, top + h));
                // Dibujar 3 barras de histograma
                g2d.draw(new Rectangle2D.Double(left + w * 0.15, top + h * 0.4, w * 0.2, h * 0.6));
                g2d.draw(new Rectangle2D.Double(left + w * 0.4, top + h * 0.1, w * 0.2, h * 0.9));
                g2d.draw(new Rectangle2D.Double(left + w * 0.65, top + h * 0.3, w * 0.2, h * 0.7));
                break;

            default:
                // Un círculo genérico
                g2d.draw(new Ellipse2D.Double(left, top, w, h));
                break;
        }

        g2d.dispose();
    }
}
