package com.programacion.rasterizer;

/**
 * Pipeline de operaciones sobre fragmentos (Capítulo 8).
 * Realiza pruebas secuenciales antes de escribir en el Framebuffer.
 */
public class FragmentPipeline implements FragmentConsumer {

    public enum ComparisonFunction {
        ALWAYS, NEVER, LESS, EQUAL, LEQUAL, GREATER, NOTEQUAL, GEQUAL
    }

    private final SoftwareRasterizer rasterizer;

    // Configuración del Scissor Test
    private boolean scissorEnabled = false;
    private int scissorX = 0;
    private int scissorY = 0;
    private int scissorWidth = 100;
    private int scissorHeight = 100;

    // Configuración del Alpha Test
    private boolean alphaTestEnabled = false;
    private int alphaReference = 128; // Rango 0-255
    private ComparisonFunction alphaFunction = ComparisonFunction.GREATER;

    // Configuración del Depth Test
    private boolean depthTestEnabled = true;
    private boolean depthWriteEnabled = true;
    private ComparisonFunction depthFunction = ComparisonFunction.LESS;

    // Configuración de MSAA
    private boolean msaaEnabled = false;

    /**
     * Crea un pipeline de fragmentos asociado a un rasterizador.
     *
     * @param rasterizer El rasterizador de software origen.
     */
    public FragmentPipeline(SoftwareRasterizer rasterizer) {
        this.rasterizer = rasterizer;
    }

    // --- GETTERS Y SETTERS ---

    public boolean isScissorEnabled() { return scissorEnabled; }
    public void setScissorEnabled(boolean scissorEnabled) { this.scissorEnabled = scissorEnabled; }

    public int getScissorX() { return scissorX; }
    public void setScissorX(int scissorX) { this.scissorX = scissorX; }

    public int getScissorY() { return scissorY; }
    public void setScissorY(int scissorY) { this.scissorY = scissorY; }

    public int getScissorWidth() { return scissorWidth; }
    public void setScissorWidth(int scissorWidth) { this.scissorWidth = scissorWidth; }

    public int getScissorHeight() { return scissorHeight; }
    public void setScissorHeight(int scissorHeight) { this.scissorHeight = scissorHeight; }

    public boolean isAlphaTestEnabled() { return alphaTestEnabled; }
    public void setAlphaTestEnabled(boolean alphaTestEnabled) { this.alphaTestEnabled = alphaTestEnabled; }

    public int getAlphaReference() { return alphaReference; }
    public void setAlphaReference(int alphaReference) { this.alphaReference = alphaReference; }

    public ComparisonFunction getAlphaFunction() { return alphaFunction; }
    public void setAlphaFunction(ComparisonFunction alphaFunction) { this.alphaFunction = alphaFunction; }

    public boolean isDepthTestEnabled() { return depthTestEnabled; }
    public void setDepthTestEnabled(boolean depthTestEnabled) { this.depthTestEnabled = depthTestEnabled; }

    public boolean isDepthWriteEnabled() { return depthWriteEnabled; }
    public void setDepthWriteEnabled(boolean depthWriteEnabled) { this.depthWriteEnabled = depthWriteEnabled; }

    public ComparisonFunction getDepthFunction() { return depthFunction; }
    public void setDepthFunction(ComparisonFunction depthFunction) { this.depthFunction = depthFunction; }

    public boolean isMsaaEnabled() { return msaaEnabled; }
    public void setMsaaEnabled(boolean msaaEnabled) { this.msaaEnabled = msaaEnabled; }

    // --- EVALUADORES DE PRUEBAS ---

    private boolean evalAlphaTest(int alpha) {
        switch (alphaFunction) {
            case ALWAYS: return true;
            case NEVER: return false;
            case LESS: return alpha < alphaReference;
            case EQUAL: return alpha == alphaReference;
            case LEQUAL: return alpha <= alphaReference;
            case GREATER: return alpha > alphaReference;
            case NOTEQUAL: return alpha != alphaReference;
            case GEQUAL: return alpha >= alphaReference;
            default: return true;
        }
    }

    private boolean evalDepthTest(double fragmentDepth, double currentDepth) {
        switch (depthFunction) {
            case ALWAYS: return true;
            case NEVER: return false;
            case LESS: return fragmentDepth < currentDepth;
            case EQUAL: return Math.abs(fragmentDepth - currentDepth) < 1e-9;
            case LEQUAL: return fragmentDepth <= currentDepth;
            case GREATER: return fragmentDepth > currentDepth;
            case NOTEQUAL: return Math.abs(fragmentDepth - currentDepth) >= 1e-9;
            case GEQUAL: return fragmentDepth >= currentDepth;
            default: return true;
        }
    }

    private int blendColor(int dest, int src, double coverage) {
        int srcA = (src >> 24) & 0xFF;
        int srcR = (src >> 16) & 0xFF;
        int srcG = (src >> 8) & 0xFF;
        int srcB = src & 0xFF;

        int destA = (dest >> 24) & 0xFF;
        int destR = (dest >> 16) & 0xFF;
        int destG = (dest >> 8) & 0xFF;
        int destB = dest & 0xFF;

        // Opacidad efectiva escalada por la cobertura
        double f = (srcA / 255.0) * coverage;

        int r = (int) (srcR * f + destR * (1.0 - f));
        int g = (int) (srcG * f + destG * (1.0 - f));
        int b = (int) (srcB * f + destB * (1.0 - f));
        int a = (int) (srcA * coverage + destA * (1.0 - f));

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // --- PIPELINE SECUENCIAL ---

    @Override
    public void consume(Fragment fragment) {
        // Validar límites del Framebuffer
        int w = rasterizer.getWidth();
        int h = rasterizer.getHeight();
        if (fragment.x < 0 || fragment.x >= w || fragment.y < 0 || fragment.y >= h) {
            fragment.discarded = true;
            return;
        }

        // 1. Scissor Test (Prueba de Recorte)
        if (scissorEnabled) {
            if (fragment.x < scissorX || fragment.x >= scissorX + scissorWidth ||
                fragment.y < scissorY || fragment.y >= scissorY + scissorHeight) {
                fragment.discarded = true;
                return;
            }
        }

        // 2. Alpha Test (Prueba de Opacidad)
        if (alphaTestEnabled) {
            int alpha = (fragment.color >> 24) & 0xFF;
            if (!evalAlphaTest(alpha)) {
                fragment.discarded = true;
                return;
            }
        }

        // 3. Multisample (MSAA)
        if (msaaEnabled && fragment.coverage < 1.0) {
            int index = fragment.y * w + fragment.x;
            int destColor = rasterizer.getPixels()[index];
            fragment.color = blendColor(destColor, fragment.color, fragment.coverage);
        }

        // 4. Depth Test (Prueba de Profundidad)
        if (depthTestEnabled && rasterizer.isZBufferEnabled()) {
            int index = fragment.y * w + fragment.x;
            double currentDepth = rasterizer.isWBufferEnabled() ? rasterizer.getWBuffer()[index] : rasterizer.getZBuffer()[index];
            double fragmentDepth = rasterizer.isWBufferEnabled() ? fragment.w : fragment.z;

            if (!evalDepthTest(fragmentDepth, currentDepth)) {
                fragment.discarded = true;
                return;
            }

            // Actualizar buffer de profundidad
            if (depthWriteEnabled) {
                if (rasterizer.isWBufferEnabled()) {
                    rasterizer.getWBuffer()[index] = fragment.w;
                } else {
                    rasterizer.getZBuffer()[index] = fragment.z;
                }
            }
        }

        // 5. Escribir color final en el Framebuffer
        int index = fragment.y * w + fragment.x;
        rasterizer.getPixels()[index] = fragment.color;
    }
}
