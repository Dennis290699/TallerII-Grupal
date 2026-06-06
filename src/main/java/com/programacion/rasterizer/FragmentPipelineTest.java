package com.programacion.rasterizer;

import java.awt.image.BufferedImage;

/**
 * Programa de prueba automatizado independiente para verificar las operaciones de FragmentPipeline.
 */
public class FragmentPipelineTest {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE FRAGMENTPIPELINE ===");
        
        try {
            testScissorTest();
            System.out.println("[OK] Scissor Test pasado con éxito.");

            testAlphaTest();
            System.out.println("[OK] Alpha Test pasado con éxito.");

            testDepthTest();
            System.out.println("[OK] Depth Test pasado con éxito.");

            testMsaaBlending();
            System.out.println("[OK] MSAA Blending pasado con éxito.");

            testStencilTest();
            System.out.println("[OK] Stencil Test pasado con éxito.");

            testBlending();
            System.out.println("[OK] Blending pasado con éxito.");

            testLogicOp();
            System.out.println("[OK] Logic Op pasado con éxito.");

            System.out.println("\n=== TODAS LAS PRUEBAS COMPLETADAS CON ÉXITO ===");
        } catch (Throwable t) {
            System.err.println("\n[ERROR] Falló alguna de las verificaciones:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testScissorTest() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(false);
        pipeline.setScissorEnabled(true);
        pipeline.setScissorX(10);
        pipeline.setScissorY(10);
        pipeline.setScissorWidth(50);
        pipeline.setScissorHeight(50);

        // Caso 1: Fragmento dentro del scissor
        Fragment inside = new Fragment(20, 20, 0.5, 0xFFFFFFFF);
        pipeline.consume(inside);
        if (inside.discarded) {
            throw new AssertionError("El fragmento dentro del Scissor Box no debió ser descartado.");
        }

        // Caso 2: Fragmento fuera del scissor
        Fragment outside = new Fragment(5, 5, 0.5, 0xFFFFFFFF);
        pipeline.consume(outside);
        if (!outside.discarded) {
            throw new AssertionError("El fragmento fuera del Scissor Box debió ser descartado.");
        }
    }

    private static void testAlphaTest() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(false);
        pipeline.setAlphaTestEnabled(true);
        pipeline.setAlphaReference(128);
        pipeline.setAlphaFunction(FragmentPipeline.ComparisonFunction.GREATER);

        // Caso 1: Alpha 200 (mayor a 128) -> Pasa
        Fragment pass = new Fragment(5, 5, 0.5, (200 << 24) | 0xFFFFFF);
        pipeline.consume(pass);
        if (pass.discarded) {
            throw new AssertionError("El fragmento con alpha 200 debió pasar el test GREATER 128.");
        }

        // Caso 2: Alpha 100 (menor a 128) -> Descartado
        Fragment discard = new Fragment(5, 5, 0.5, (100 << 24) | 0xFFFFFF);
        pipeline.consume(discard);
        if (!discard.discarded) {
            throw new AssertionError("El fragmento con alpha 100 debió ser descartado en el test GREATER 128.");
        }
    }

    private static void testDepthTest() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);
        sr.setZBufferEnabled(true);
        sr.setWBufferEnabled(false); // Usar Z-Buffer clásico
        sr.clearZBuffer(); // Inicializa con Double.MAX_VALUE

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(true);
        pipeline.setDepthWriteEnabled(true);
        pipeline.setDepthFunction(FragmentPipeline.ComparisonFunction.LESS);

        int index = 5 * 10 + 5;
        sr.getZBuffer()[index] = 5.0; // Establecer profundidad inicial

        // Caso 1: Profundidad 2.0 (menor a 5.0) -> Pasa
        Fragment pass = new Fragment(5, 5, 2.0, 0xFFFFFFFF);
        pipeline.consume(pass);
        if (pass.discarded) {
            throw new AssertionError("El fragmento con Z=2.0 debió pasar el test LESS contra Z=5.0.");
        }
        if (sr.getZBuffer()[index] != 2.0) {
            throw new AssertionError("La escritura de profundidad falló, el Z-buffer debería valer 2.0 pero vale " + sr.getZBuffer()[index]);
        }

        // Caso 2: Profundidad 8.0 (mayor a 2.0) -> Descartado
        Fragment discard = new Fragment(5, 5, 8.0, 0xFFFFFFFF);
        pipeline.consume(discard);
        if (!discard.discarded) {
            throw new AssertionError("El fragmento con Z=8.0 debió ser descartado contra Z=2.0.");
        }
        if (sr.getZBuffer()[index] != 2.0) {
            throw new AssertionError("El Z-buffer no debió alterarse en un test fallido.");
        }
    }

    private static void testMsaaBlending() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);
        
        // Colorear el fondo de negro completamente opaco
        sr.clearColorBuffer(0xFF000000); 

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setMsaaEnabled(true);
        pipeline.setDepthTestEnabled(false); // Deshabilitar para no interferir

        // Fragmento con color blanco puro opaco (0xFFFFFFFF) y cobertura 0.5
        Fragment frag = new Fragment(5, 5, 0.5, 0xFFFFFFFF);
        frag.coverage = 0.5;

        pipeline.consume(frag);

        int resultColor = sr.getPixels()[5 * 10 + 5];
        
        // Esperamos mezcla 50% entre 0x00 y 0xFF -> aprox 127 o 128
        int r = (resultColor >> 16) & 0xFF;
        int g = (resultColor >> 8) & 0xFF;
        int b = resultColor & 0xFF;

        if (Math.abs(r - 127) > 2 || Math.abs(g - 127) > 2 || Math.abs(b - 127) > 2) {
            throw new AssertionError("El color mezclado por MSAA no es el esperado. Esperado cerca de 127 para R,G,B pero fue: R=" + r + ", G=" + g + ", B=" + b);
        }
    }

    private static void testStencilTest() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);
        
        sr.clearStencilBuffer((byte) 0);

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(false);
        pipeline.setStencilEnabled(true);
        pipeline.setStencilReference(5);
        pipeline.setStencilFunction(FragmentPipeline.ComparisonFunction.EQUAL);
        pipeline.setStencilFailOp(FragmentPipeline.StencilOp.INCR);
        pipeline.setStencilPassOp(FragmentPipeline.StencilOp.KEEP);

        int index = 5 * 10 + 5;

        // Caso 1: Ref=5, Buffer=0 -> EQUAL falla. 
        // Se debe descartar y aplicar Stencil Fail: INCR. El buffer debe subir a 1.
        Fragment f1 = new Fragment(5, 5, 0.5, 0xFFFFFFFF);
        pipeline.consume(f1);
        if (!f1.discarded) {
            throw new AssertionError("El fragmento debió fallar la prueba de stencil EQUAL (5 != 0).");
        }
        if ((sr.getStencilBuffer()[index] & 0xFF) != 1) {
            throw new AssertionError("El buffer stencil debió incrementarse a 1 tras fallo de stencil.");
        }

        // Caso 2: Cambiar a función ALWAYS, Ref=5, PassOp=REPLACE.
        // Debe pasar y reemplazar el stencil por la referencia (5).
        pipeline.setStencilFunction(FragmentPipeline.ComparisonFunction.ALWAYS);
        pipeline.setStencilPassOp(FragmentPipeline.StencilOp.REPLACE);
        Fragment f2 = new Fragment(5, 5, 0.5, 0xFFFFFFFF);
        pipeline.consume(f2);
        if (f2.discarded) {
            throw new AssertionError("El fragmento debió pasar la prueba stencil ALWAYS.");
        }
        if ((sr.getStencilBuffer()[index] & 0xFF) != 5) {
            throw new AssertionError("El buffer stencil debió actualizarse al valor de referencia 5.");
        }
    }

    private static void testBlending() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(false);

        int index = 5 * 10 + 5;

        // Caso 1: BlendingMode.TRANSPARENCY
        // Fondo: Negro (0xFF000000). Origen: 50% Rojo semi-transparente (0x80FF0000).
        sr.clearColorBuffer(0xFF000000);
        pipeline.setBlendingMode(FragmentPipeline.BlendingMode.TRANSPARENCY);
        Fragment f1 = new Fragment(5, 5, 0.5, 0x80FF0000);
        pipeline.consume(f1);
        int c1 = sr.getPixels()[index];
        int r1 = (c1 >> 16) & 0xFF;
        // Esperado: 255 * (128/255.0) = 128
        if (Math.abs(r1 - 128) > 2) {
            throw new AssertionError("Fallo en transparencia: esperado R ~ 128, obtenido " + r1);
        }

        // Caso 2: BlendingMode.ADDITIVE
        // Fondo: Verde Oscuro (0xFF004000). Origen: 50% Verde (0x80008000 -> 0x80 alfa, 0x80 verde).
        // C_out = C_src * effSrcA + C_dst -> 0x80 * 0.5 + 0x40 = 0x40 + 0x40 = 0x80 (128)
        sr.clearColorBuffer(0xFF004000);
        pipeline.setBlendingMode(FragmentPipeline.BlendingMode.ADDITIVE);
        Fragment f2 = new Fragment(5, 5, 0.5, 0x80008000);
        pipeline.consume(f2);
        int c2 = sr.getPixels()[index];
        int g2 = (c2 >> 8) & 0xFF;
        if (Math.abs(g2 - 128) > 2) {
            throw new AssertionError("Fallo en aditivo: esperado G ~ 128, obtenido " + g2);
        }

        // Caso 3: BlendingMode.MULTIPLICATIVE
        // Fondo: Amarillo (0xFFFFFF00). Origen: Cian (0xFF00FFFF).
        // R = 255 * 0 / 255 = 0
        // G = 255 * 255 / 255 = 255
        // B = 0 * 255 / 255 = 0
        // Result: 0xFF00FF00 (Verde)
        sr.clearColorBuffer(0xFFFFFF00);
        pipeline.setBlendingMode(FragmentPipeline.BlendingMode.MULTIPLICATIVE);
        Fragment f3 = new Fragment(5, 5, 0.5, 0xFF00FFFF);
        pipeline.consume(f3);
        int c3 = sr.getPixels()[index];
        if (c3 != 0xFF00FF00) {
            throw new AssertionError("Fallo en multiplicativo: esperado 0xFF00FF00, obtenido " + Integer.toHexString(c3));
        }
    }

    private static void testLogicOp() {
        SoftwareRasterizer sr = new SoftwareRasterizer();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        sr.bindTarget(img);

        FragmentPipeline pipeline = new FragmentPipeline(sr);
        pipeline.setDepthTestEnabled(false);
        pipeline.setLogicOpEnabled(true);
        pipeline.setLogicOp(FragmentPipeline.LogicOpMode.XOR);

        int index = 5 * 10 + 5;

        // XOR: 0xFF00FF00 ^ 0xFF00FFFF = 0x000000FF (azul totalmente transparente)
        sr.clearColorBuffer(0xFF00FF00);
        Fragment f1 = new Fragment(5, 5, 0.5, 0xFF00FFFF);
        pipeline.consume(f1);
        int c1 = sr.getPixels()[index];
        if (c1 != 0x000000FF) {
            throw new AssertionError("Fallo en LogicOp XOR: esperado 0x000000FF, obtenido " + Integer.toHexString(c1));
        }
    }
}
