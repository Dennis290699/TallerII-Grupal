package com.programacion.rasterizer;

/**
 * Pipeline de operaciones sobre fragmentos (Capítulo 8 y 9).
 * Realiza pruebas secuenciales y de mezcla antes de escribir en el Framebuffer.
 */
public class FragmentPipeline implements FragmentConsumer {

    public enum ComparisonFunction {
        ALWAYS, NEVER, LESS, EQUAL, LEQUAL, GREATER, NOTEQUAL, GEQUAL
    }

    public enum StencilOp {
        KEEP, ZERO, REPLACE, INCR, INCR_WRAP, DECR, DECR_WRAP, INVERT
    }

    public enum BlendingMode {
        NONE, TRANSPARENCY, ADDITIVE, MULTIPLICATIVE
    }

    public enum LogicOpMode {
        CLEAR, AND, AND_REVERSE, COPY, AND_INVERTED, NOOP, XOR, OR, NOR, EQUIV, INVERT, OR_REVERSE, COPY_INVERTED, OR_INVERTED, NAND, SET
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

    // Configuración de Stencil Test (Capítulo 9)
    private boolean stencilEnabled = false;
    private ComparisonFunction stencilFunction = ComparisonFunction.ALWAYS;
    private int stencilReference = 0;
    private int stencilMask = 0xFF;
    private int stencilWriteMask = 0xFF;
    private StencilOp stencilFailOp = StencilOp.KEEP;
    private StencilOp stencilDepthFailOp = StencilOp.KEEP;
    private StencilOp stencilPassOp = StencilOp.KEEP;

    // Configuración de Blending (Capítulo 9)
    private BlendingMode blendingMode = BlendingMode.NONE;

    // Configuración de Logic Op (Capítulo 9)
    private boolean logicOpEnabled = false;
    private LogicOpMode logicOp = LogicOpMode.COPY;

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

    // Getters y Setters de Stencil (Capítulo 9)
    public boolean isStencilEnabled() { return stencilEnabled; }
    public void setStencilEnabled(boolean stencilEnabled) { this.stencilEnabled = stencilEnabled; }

    public ComparisonFunction getStencilFunction() { return stencilFunction; }
    public void setStencilFunction(ComparisonFunction stencilFunction) { this.stencilFunction = stencilFunction; }

    public int getStencilReference() { return stencilReference; }
    public void setStencilReference(int stencilReference) { this.stencilReference = stencilReference; }

    public int getStencilMask() { return stencilMask; }
    public void setStencilMask(int stencilMask) { this.stencilMask = stencilMask; }

    public int getStencilWriteMask() { return stencilWriteMask; }
    public void setStencilWriteMask(int stencilWriteMask) { this.stencilWriteMask = stencilWriteMask; }

    public StencilOp getStencilFailOp() { return stencilFailOp; }
    public void setStencilFailOp(StencilOp stencilFailOp) { this.stencilFailOp = stencilFailOp; }

    public StencilOp getStencilDepthFailOp() { return stencilDepthFailOp; }
    public void setStencilDepthFailOp(StencilOp stencilDepthFailOp) { this.stencilDepthFailOp = stencilDepthFailOp; }

    public StencilOp getStencilPassOp() { return stencilPassOp; }
    public void setStencilPassOp(StencilOp stencilPassOp) { this.stencilPassOp = stencilPassOp; }

    // Getters y Setters de Blending y Logic Ops (Capítulo 9)
    public BlendingMode getBlendingMode() { return blendingMode; }
    public void setBlendingMode(BlendingMode blendingMode) { this.blendingMode = blendingMode; }

    public boolean isLogicOpEnabled() { return logicOpEnabled; }
    public void setLogicOpEnabled(boolean logicOpEnabled) { this.logicOpEnabled = logicOpEnabled; }

    public LogicOpMode getLogicOp() { return logicOp; }
    public void setLogicOp(LogicOpMode logicOp) { this.logicOp = logicOp; }

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

    private boolean evalStencilTest(int ref, int bufferVal) {
        int r = ref & stencilMask;
        int b = bufferVal & stencilMask;
        switch (stencilFunction) {
            case ALWAYS: return true;
            case NEVER: return false;
            case LESS: return r < b;
            case EQUAL: return r == b;
            case LEQUAL: return r <= b;
            case GREATER: return r > b;
            case NOTEQUAL: return r != b;
            case GEQUAL: return r >= b;
            default: return true;
        }
    }

    private int calculateStencilOpValue(StencilOp op, int currentVal) {
        switch (op) {
            case ZERO: return 0;
            case REPLACE: return stencilReference;
            case INCR: return Math.min(255, currentVal + 1);
            case INCR_WRAP: return (currentVal + 1) & 0xFF;
            case DECR: return Math.max(0, currentVal - 1);
            case DECR_WRAP: return (currentVal - 1) & 0xFF;
            case INVERT: return (~currentVal) & 0xFF;
            case KEEP:
            default: return currentVal;
        }
    }

    private void updateStencilBuffer(int index, StencilOp op) {
        if (rasterizer.getStencilBuffer() == null) return;
        int currentVal = rasterizer.getStencilBuffer()[index] & 0xFF;
        int newVal = calculateStencilOpValue(op, currentVal);
        int maskedVal = (currentVal & ~stencilWriteMask) | (newVal & stencilWriteMask);
        rasterizer.getStencilBuffer()[index] = (byte) (maskedVal & 0xFF);
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

        double effSrcA = (srcA / 255.0) * coverage;

        int r, g, b, a;

        switch (blendingMode) {
            case TRANSPARENCY:
                double fDst = 1.0 - effSrcA;
                r = (int) (srcR * effSrcA + destR * fDst);
                g = (int) (srcG * effSrcA + destG * fDst);
                b = (int) (srcB * effSrcA + destB * fDst);
                a = (int) ((srcA * coverage) + destA * fDst);
                break;
            case ADDITIVE:
                r = (int) (srcR * effSrcA + destR);
                g = (int) (srcG * effSrcA + destG);
                b = (int) (srcB * effSrcA + destB);
                a = (int) ((srcA * coverage) + destA);
                break;
            case MULTIPLICATIVE:
                r = (int) ((srcR / 255.0) * destR);
                g = (int) ((srcG / 255.0) * destG);
                b = (int) ((srcB / 255.0) * destB);
                a = (int) ((srcA / 255.0) * destA);
                break;
            case NONE:
            default:
                if (msaaEnabled && coverage < 1.0) {
                    r = (int) (srcR * coverage + destR * (1.0 - coverage));
                    g = (int) (srcG * coverage + destG * (1.0 - coverage));
                    b = (int) (srcB * coverage + destB * (1.0 - coverage));
                    a = (int) (srcA * coverage + destA * (1.0 - coverage));
                } else {
                    return src;
                }
                break;
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int applyLogicOp(int src, int dest) {
        switch (logicOp) {
            case CLEAR: return 0xFF000000;
            case SET: return 0xFFFFFFFF;
            case COPY: return src;
            case COPY_INVERTED: return ~src;
            case NOOP: return dest;
            case INVERT: return ~dest;
            case AND: return src & dest;
            case NAND: return ~(src & dest);
            case OR: return src | dest;
            case NOR: return ~(src | dest);
            case XOR: return src ^ dest;
            case EQUIV: return ~(src ^ dest);
            case AND_REVERSE: return src & ~dest;
            case AND_INVERTED: return ~src & dest;
            case OR_REVERSE: return src | ~dest;
            case OR_INVERTED: return ~src | dest;
            default: return src;
        }
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

        int index = fragment.y * w + fragment.x;

        // 1. Scissor Test (Prueba de Recorte)
        if (scissorEnabled) {
            if (fragment.x < scissorX || fragment.x >= scissorX + scissorWidth ||
                fragment.y < scissorY || fragment.y >= scissorY + scissorHeight) {
                fragment.discarded = true;
                return;
            }
        }

        // 2. Stencil Test
        boolean stencilPassed = true;
        if (stencilEnabled && rasterizer.getStencilBuffer() != null) {
            int currentStencil = rasterizer.getStencilBuffer()[index] & 0xFF;
            stencilPassed = evalStencilTest(stencilReference, currentStencil);
            if (!stencilPassed) {
                updateStencilBuffer(index, stencilFailOp);
                fragment.discarded = true;
                return;
            }
        }

        // 3. Depth Test
        boolean depthPassed = true;
        if (depthTestEnabled && rasterizer.isZBufferEnabled()) {
            double currentDepth = rasterizer.isWBufferEnabled() ? rasterizer.getWBuffer()[index] : rasterizer.getZBuffer()[index];
            double fragmentDepth = rasterizer.isWBufferEnabled() ? fragment.w : fragment.z;

            depthPassed = evalDepthTest(fragmentDepth, currentDepth);

            if (!depthPassed) {
                if (stencilEnabled && rasterizer.getStencilBuffer() != null) {
                    updateStencilBuffer(index, stencilDepthFailOp);
                }
                fragment.discarded = true;
                return;
            }
        }

        // Si llegó aquí, ambos tests (Stencil y Depth) pasaron.
        if (stencilEnabled && rasterizer.getStencilBuffer() != null) {
            updateStencilBuffer(index, stencilPassOp);
        }

        // Actualizar buffer de profundidad si depth pass
        if (depthTestEnabled && rasterizer.isZBufferEnabled() && depthWriteEnabled) {
            if (rasterizer.isWBufferEnabled()) {
                rasterizer.getWBuffer()[index] = fragment.w;
            } else {
                rasterizer.getZBuffer()[index] = fragment.z;
            }
        }

        // 4. Alpha Test
        if (alphaTestEnabled) {
            int alpha = (fragment.color >> 24) & 0xFF;
            if (!evalAlphaTest(alpha)) {
                fragment.discarded = true;
                return;
            }
        }

        // 5. Blending & Logic Op
        int destColor = rasterizer.getPixels()[index];
        int finalColor;

        if (logicOpEnabled && logicOp != LogicOpMode.COPY) {
            finalColor = applyLogicOp(fragment.color, destColor);
        } else if (blendingMode != BlendingMode.NONE) {
            finalColor = blendColor(destColor, fragment.color, fragment.coverage);
        } else if (msaaEnabled && fragment.coverage < 1.0) {
            finalColor = blendColor(destColor, fragment.color, fragment.coverage);
        } else {
            finalColor = fragment.color;
        }

        rasterizer.getPixels()[index] = finalColor;
    }
}

