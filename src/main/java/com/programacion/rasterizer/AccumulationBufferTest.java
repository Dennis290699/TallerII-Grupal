package com.programacion.rasterizer;

/**
 * Programa de prueba automatizado independiente para verificar las operaciones de AccumulationBuffer.
 */
public class AccumulationBufferTest {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE ACCUMULATIONBUFFER ===");
        try {
            testAccumulateAndReturn();
            System.out.println("[OK] Prueba de acumulación y retorno básica pasada con éxito.");

            testScaleAccumulation();
            System.out.println("[OK] Prueba de escala de acumulación pasada con éxito.");

            testResizeAndClear();
            System.out.println("[OK] Prueba de redimensionamiento y limpieza pasada con éxito.");

            System.out.println("\n=== TODAS LAS PRUEBAS DE ACCUMULATIONBUFFER COMPLETADAS CON ÉXITO ===");
        } catch (Throwable t) {
            System.err.println("\n[ERROR] Falló alguna de las verificaciones de AccumulationBuffer:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testAccumulateAndReturn() {
        AccumulationBuffer buffer = new AccumulationBuffer(2, 2);
        
        // Píxeles de prueba: 2x2
        // Pixel 0: 0xFF102030 (A=255, R=16, G=32, B=48)
        // Pixel 1: 0x80405060 (A=128, R=64, G=80, B=96)
        // Pixel 2: 0x00000000
        // Pixel 3: 0xFFFFFFFF
        int[] pixels = { 0xFF102030, 0x80405060, 0x00000000, 0xFFFFFFFF };

        // Acumular 2 veces con peso 0.5f -> Debería promediarse exactamente a los valores originales (si se suma 0.5 + 0.5 = 1.0)
        buffer.accumulate(pixels, 0.5f);
        buffer.accumulate(pixels, 0.5f);

        int[] destPixels = new int[4];
        buffer.returnFrame(destPixels, 1.0f);

        for (int i = 0; i < 4; i++) {
            if (destPixels[i] != pixels[i]) {
                throw new AssertionError(String.format("Error de acumulación en pixel %d. Esperado %08X, obtenido %08X", i, pixels[i], destPixels[i]));
            }
        }
    }

    private static void testScaleAccumulation() {
        AccumulationBuffer buffer = new AccumulationBuffer(1, 1);
        int[] pixels = { 0xFF808080 }; // A=255, R=128, G=128, B=128
        
        buffer.accumulate(pixels, 1.0f);
        buffer.scaleAccumulation(0.5f); // Atenuar al 50%
        
        int[] dest = new int[1];
        buffer.returnFrame(dest, 1.0f);

        int r = (dest[0] >> 16) & 0xFF;
        int g = (dest[0] >> 8) & 0xFF;
        int b = dest[0] & 0xFF;
        int a = (dest[0] >> 24) & 0xFF;

        if (r != 64 || g != 64 || b != 64 || a != 128) {
            throw new AssertionError(String.format("Error en escala. Esperado A=128, R=64, G=64, B=64, obtenido A=%d, R=%d, G=%d, B=%d", a, r, g, b));
        }
    }

    private static void testResizeAndClear() {
        AccumulationBuffer buffer = new AccumulationBuffer(10, 10);
        buffer.resize(20, 20);

        if (buffer.getWidth() != 20 || buffer.getHeight() != 20) {
            throw new AssertionError("El redimensionamiento del buffer falló.");
        }

        int[] pixels = new int[400];
        java.util.Arrays.fill(pixels, 0xFFFFFFFF);
        buffer.accumulate(pixels, 1.0f);
        buffer.clear();

        float[] r = buffer.getAccumR();
        for (float val : r) {
            if (val != 0.0f) {
                throw new AssertionError("La limpieza del buffer de acumulación no borró los valores a cero.");
            }
        }
    }
}
