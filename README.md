# LuminaFX - Procesamiento de Imágenes y Renderizado 3D por Software

**LuminaFX** es una aplicación académica y profesional desarrollada en Java utilizando la biblioteca gráfica Swing y el framework de diseño FlatLaf. Combina herramientas avanzadas de edición, filtrado y composición de imágenes en 2D con un motor de rasterización 3D por software completo y un pipeline de operaciones de fragmento secuenciales que replica de forma física y didáctica el comportamiento de APIs de bajo nivel de GPU como OpenGL.

El proyecto destaca por implementar su propio pipeline gráfico tridimensional por software (CPU) sin depender de bibliotecas externas de aceleración por hardware, manipulando la memoria del búfer de color directamente a nivel de píxel.

---

## 👥 Integrantes del Equipo y Roles

| Integrante | GitHub | Capítulos Asignados | Contribuciones Principales |
| :--- | :--- | :--- | :--- |
| **Dennis** | [@Dennis290699](https://github.com/Dennis290699) | Capítulos 6 y 7 | Motor de Rasterización por Software 3D, trazado de líneas (Bresenham/DDA), triángulos baricéntricos, mapeo de texturas bilineal y corrección de perspectiva. |
| **Freddy** | [@XavierT1](https://github.com/XavierT1) | Capítulos 8 y 9 | Pipeline de operaciones sobre fragmentos (Scissor, Alpha, Depth y Stencil Test, Blending de fragmentos, Logic Ops y 4x MSAA). |
| **Kevin Fernando Pozo Maldo** | [@kevin](https://github.com) | Capítulo 10 y README | Buffer de Acumulación (Accumulation Buffer) con precisión flotante, post-procesamiento de efectos avanzados (Motion Blur temporal/offline, Depth of Field y FSAA) y documentación técnica del proyecto completo. |

---

## 📌 Índice de Capítulos

- [Capítulo 6: Rasterización (Z-Buffer, Bitmaps, Píxeles)](#capítulo-6-rasterización-z-buffer-bitmaps-píxeles)
- [Capítulo 7: Texturas, Color, Interpolación en Profundidad y W-Buffering](#capítulo-7-texturas-color-interpolación-en-profundidad-y-w-buffering)
- [Capítulo 8: Fragmentos (Operaciones, Multisample, Alpha Test)](#capítulo-8-fragmentos-operaciones-multisample-alpha-test)
- [Capítulo 9: Operaciones con Fragmentos Avanzado (Stencil Test, Blending, Logic Op)](#capítulo-9-operaciones-con-fragmentos-avanzado-stencil-test-blending-logic-op)
- [Capítulo 10: Buffer de Acumulación (FSAA, DoF, Motion Blur)](#capítulo-10-buffer-de-acumulación-fsaa-dof-motion-blur)

---

## 📖 Explicación Técnica Detallada del Proyecto

A continuación, se presenta la especificación detallada de cada una de las fases del motor de renderizado 3D por software implementado en **LuminaFX**:

---

### Capítulo 6: Rasterización (Z-Buffer, Bitmaps, Píxeles)
Esta fase constituye el núcleo del dibujo de primitivas 2D y 3D en pantalla. Opera directamente sobre un arreglo unidimensional de píxeles (`int[] pixels`) mapeado en memoria a una `BufferedImage` nativa de Java (`TYPE_INT_ARGB`) para un acceso ultra eficiente (sin sobrecosto de API).

* **Algoritmos para Trazar Líneas 3D:**
  - **DDA (Digital Differential Analyzer):** Realiza un cálculo de incrementos fraccionarios de forma secuencial interpolando la profundidad linealmente en base a la pendiente.
  - **Bresenham:** Utiliza aritmética entera de incrementos rápidos para tomar decisiones de avance de píxel, minimizando el costo computacional de coma flotante.
* **Rasterización de Triángulos:**
  - Emplea un algoritmo de **caja delimitadora (Bounding Box)** que calcula el rectángulo mínimo ocupado por el triángulo y lo recorta contra los límites de pantalla.
  - Para cada píxel dentro del rectángulo, calcula sus **coordenadas baricéntricas** ($w_1, w_2, w_3$). Si los tres coeficientes son no-negativos ($\ge -10^{-9}$), el píxel pertenece al triángulo y es enviado al pipeline de fragmentos.
* **Z-Buffer (Prueba de Profundidad):**
  - Mantiene un buffer de profundidad (`double[] zBuffer`) del tamaño de la pantalla.
  - Durante la rasterización, se calcula la profundidad $Z$ interpolada de cada píxel mediante los pesos baricéntricos. Si la prueba está activa, el fragmento se escribe solo si su valor $Z$ es menor que el almacenado en el buffer, resolviendo el problema de la visibilidad y oclusión de superficies.
  - **Z-Buffer visual:** Permite exportar los valores de profundidad a una escala de grises para inspección didáctica (objetos más cercanos se visualizan más brillantes).

---

### Capítulo 7: Texturas, Color, Interpolación en Profundidad y W-Buffering
Amplía la rasterización para soportar color continuo (sombreado) y envolturas de imágenes (texturizado) con corrección matemática en perspectiva.

* **Sombreado por Color e Iluminación:**
  - Soporta **Flat Shading** (sombreado plano por cara calculando la normal del triángulo contra una dirección de luz) y **Gouraud Shading** (iluminación interpolada por vértice a través de pesos baricéntricos).
* **Mapeo de Texturas (Texture Mapping):**
  - Permite envolver triángulos con imágenes en 2D mediante coordenadas normalizadas $U, V \in [0, 1]$.
  - Implementa **Filtrado Bilineal (Bilinear Filtering):** Muestrea y promedia ponderadamente los 4 píxeles más cercanos de la textura (texels) para evitar la pixelación y aliasing al acercar la cámara al objeto.
* **W-Buffering (Corrección de Perspectiva):**
  - En una proyección en perspectiva, la distancia en pantalla no escala linealmente con la profundidad real del espacio 3D.
  - Para evitar la distorsión geométrica de las texturas, se aplica **W-Buffering**: todos los atributos de los vértices (coordenadas de textura $U, V$, componentes de color e intensidades de luz) se dividen por la coordenada homogénea de profundidad $W$ (distancia real al plano de la cámara).
  - Los atributos recíprocos ($1/W, U/W, V/W$) se interpolan linealmente en el espacio 2D de la pantalla mediante coordenadas baricéntricas y luego se recupera el valor corregido dividiendo los atributos interpolados entre el $1/W$ interpolado de ese píxel.

---

### Capítulo 8: Fragmentos (Operaciones, Multisample, Alpha Test)
En esta fase, la rasterización genera **fragmentos** (píxeles potenciales con datos de color, posición y profundidad) los cuales son sometidos a un pipeline secuencial de descarte y procesamiento en la clase `FragmentPipeline`.

* **Scissor Test (Prueba de Recorte):**
  - Verifica si las coordenadas $(x, y)$ del fragmento caen dentro de un rectángulo de recorte (Scissor Box). Si están fuera, el fragmento es descartado.
* **Alpha Test (Prueba de Transparencia):**
  - Compara el canal Alfa (opacidad) del fragmento contra una referencia fija utilizando una función de comparación seleccionable (LESS, GREATER, EQUAL, etc.). Permite descartar píxeles transparentes (como rejillas o follaje).
* **Multisample Anti-Aliasing (4x MSAA):**
  - Reduce el aliasing de bordes (efecto de serrucho) dividiendo cada píxel en $2 \times 2$ subpíxeles con offsets fijos de submuestreo.
  - El rasterizador calcula cuántos de estos subpíxeles se encuentran dentro de las fronteras baricéntricas del triángulo para determinar un factor de cobertura ($0.25, 0.5, 0.75, 1.0$). Este factor se utiliza para atenuar o mezclar la opacidad final en los bordes de la geometría.

---

### Capítulo 9: Operaciones con Fragmentos Avanzado (Stencil Test, Blending, Logic Op)
Introduce operaciones avanzadas aplicadas al píxel antes de su mezcla final con el buffer de color.

* **Stencil Test (Prueba de Plantilla):**
  - Implementa un buffer de máscara de 8 bits (`byte[] stencilBuffer`).
  - Evalúa los bits del fragmento contra el valor almacenado en el búfer mediante funciones clásicas de comparación y máscaras (`stencilMask`).
  - Aplica operaciones lógicas de escritura configurables (`stencilFailOp`, `stencilDepthFailOp`, `stencilPassOp`) como KEEP, ZERO, REPLACE, INCR o DECR con máscaras de escritura para lograr efectos de portales, sombras volumétricas o siluetas.
* **Blending (Mezcla de Color):**
  - Mezcla el color del fragmento de origen (Source) con el color que ya está pintado en la pantalla (Destination).
  - Soporta modos matemáticos estándar:
    - **ALPHA:** Mezcla lineal basada en la opacidad ($C_{out} = C_{src} \cdot A_{src} + C_{dst} \cdot (1 - A_{src})$).
    - **ADDITIVE:** Suma de intensidades, ideal para efectos de brillo o fuego ($C_{out} = C_{src} \cdot A_{src} + C_{dst}$).
    - **MULTIPLICATIVE:** Multiplica componente a componente, ideal para mapeo de sombras o filtros ($C_{out} = C_{src} \cdot C_{dst}$).
* **Logic Ops (Operaciones Lógicas):**
  - Permite realizar operaciones booleanas bit a bit en binario (XOR, AND, OR, NOR, NAND, INVERT) entre los colores origen y destino, útil para técnicas gráficas clásicas de dibujo 2D (como cursor XOR interactivo).

---

### Capítulo 10: Buffer de Acumulación (FSAA, DoF, Motion Blur)
Implementa la clase `AccumulationBuffer` que proporciona un buffer de coma flotante de alta precisión (`float[] accumR, accumG, accumB, accumA`) para combinar múltiples pasadas de renderizado evitando errores de redondeo cromático.

* **Full-Scene Antialiasing (FSAA):**
  - Aplica un suavizado de bordes global sobre toda la escena.
  - Renderiza la escena $N$ veces, aplicando un jitter subpíxel (desplazamientos de fracción de píxel en la proyección de cámara $(jx, jy)$ usando una espiral áurea) en cada pasada, y promedia los resultados en el buffer de acumulación.
* **Depth of Field (Profundidad de Campo - DoF):**
  - Simula el comportamiento físico de lentes ópticas de cámaras reales.
  - Para cada pasada $i$, realiza un desplazamiento circular de la lente $(dx, dy)$ (jitter de cámara) y corrige la proyección de manera que la distancia focal $d_f$ elegida permanezca estática en pantalla. Los objetos alejados de la distancia focal sufrirán un desfase geométrico y se acumularán borrosos, logrando el efecto bokeh de desenfoque de fondo.
* **Motion Blur (Desenfoque de Movimiento):**
  - **Temporal:** Efecto de estela en tiempo real que mezcla de manera recursiva la imagen acumulada del fotograma anterior con la actual usando un decaimiento exponencial ($\text{Accum} = \text{Accum} \cdot \beta + \text{Current} \cdot (1 - \beta)$).
  - **Offline (Multi-cuadro):** Realiza una acumulación estática de $N$ subcuadros dividiendo y avanzando la rotación de los objetos de manera lineal entre fotogramas sucesivos para generar estelas uniformes de alta calidad.

---

## 🛠️ Tecnologías Utilizadas

- **Lenguaje:** Java 17 o superior.
- **Entorno Gráfico:** Java Swing / AWT (Java 2D para renderizado eficiente).
- **Estilos:** FlatLaf (Flat Dark & Extras) para la estética moderna.
- **Construcción y Dependencias:** Gradle.
- **Pruebas:** Suites de verificación unitaria autónomas para el Pipeline y el Buffer de Acumulación.

---

## 🚀 Instalación y Ejecución

Sigue estos pasos para compilar y ejecutar el proyecto de forma local:

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/Dennis290699/TallerII-Grupal.git
   cd TallerII-Grupal
   ```

2. **Compilar las clases del proyecto:**
   ```bash
   ./gradlew classes
   ```

3. **Ejecutar el programa principal:**
   ```bash
   ./gradlew run
   ```

4. **Construir el archivo JAR ejecutable (empaquetado con dependencias):**
   ```bash
   ./gradlew shadowJar
   ```
   El JAR autocontenido se generará en la ruta: `build/libs/TallerII-1.0.6.jar`.

### Ejecutar las Pruebas Unitarias Autónomas

Para ejecutar las pruebas del Pipeline de Fragmentos:
```bash
java -cp build/classes/java/main com.programacion.rasterizer.FragmentPipelineTest
```

Para ejecutar las pruebas del Buffer de Acumulación:
```bash
java -cp build/classes/java/main com.programacion.rasterizer.AccumulationBufferTest
```

---

## 🤝 Cómo Contribuir

Para mantener un flujo de trabajo ordenado, el equipo sigue estrictamente el control de versionamiento semántico:

1. **Comunicación de cambios (Issues):**
   Antes de realizar cualquier cambio, se debe abrir un **Issue** en GitHub explicando el problema o la nueva característica a agregar.

2. **Trabajo en ramas (Branching):**
   Crea una rama de trabajo a partir de `master` con un nombre descriptivo:
   ```bash
   git checkout -b feature/nombre-de-la-caracteristica
   ```

3. **Creación de Commits:**
   Realiza tus commits respetando las pautas convencionales (ej. `feat(convoluciones): agregar desenfoque gaussiano`).

4. **Vincular y cerrar el Issue (Pull Request):**
   Abre una **Pull Request (PR)** hacia la rama `master`. Para que el Issue se cierre de manera automática cuando el administrador acepte el PR, debes añadir en el cuerpo del PR la siguiente palabra clave:
   ```text
   Closes #<ID_DE_TU_ISSUE>
   ```
   *(Ejemplo: `Closes #7`)*

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo para más detalles.
