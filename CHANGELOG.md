# Registro de Cambios (Changelog)

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased] (No publicado aún)

Aquí puedes ir anotando los cambios que estás haciendo y que aún no has lanzado en una nueva versión.

### Añadido (Added)
- Nueva clase `FragmentPipeline` con pruebas de fragmento secuenciales: Scissor Test, Alpha Test y Depth Test.
- Suite de pruebas automatizada `FragmentPipelineTest` para verificación aislada de operaciones.
- Panel de configuración interactivo para el pipeline y representación gráfica del Scissor Box en `RasterizerFrame`.

### Cambiado (Changed)
- `SoftwareRasterizer` modificado para procesar fragmentos a través del pipeline e integrar cálculo baricéntrico de cobertura de 2x2 subpíxeles (4x MSAA).

---

## [1.0.6] - 2026-05-28

### Añadido (Added)
- Nuevos presets de matrices de color para daltonismo, technicolor y visión nocturna.
- Nuevas convoluciones, incluyendo desenfoque gaussiano y detectores de bordes.
- Iconografía moderna reutilizable para la interfaz de LuminaFX.
- Documentación técnica y guía del proyecto incluidas en el repositorio.

### Cambiado (Changed)
- Versión del proyecto actualizada a `1.0.6`.
- Mejora visual general de la interfaz principal, editor avanzado y flujo de Blending Multicapa.
- Blending Multicapa ampliado con visibilidad por capa, exportación al editor principal, histograma y nuevos modos de mezcla.
- Organización de herramientas en pestañas para mejorar la navegación y la experiencia de usuario.

---

## [1.0.5] - 2026-05-21

### Añadido (Added)
- Nuevo modo de Blending Multicapa para combinar dos o más imágenes.
- Gestión de capas con carga múltiple, eliminación, reordenamiento, miniaturas y selección de capa activa.
- Modos de mezcla Alpha, Sumativo y Multiplicativo con control individual de opacidad.
- Guardado del resultado combinado como imagen PNG.

### Cambiado (Changed)
- Versión del proyecto actualizada a `1.0.5`.
- Editor avanzado integrado como panel dentro de la ventana principal para alternar entre modos sin abrir ventanas separadas.
- Navegación de la ventana principal actualizada para volver al modo simple desde editores integrados.

---

## [1.0.4] - 2026-05-19

### Añadido (Added)
- Panel de previsualización con zoom y ajuste automático a pantalla.
- Panel de histograma RGB con visualización combinada y por canal.
- Ajuste HSV para modificar tono, saturación y brillo.
- Filtro de matriz de color con presets como sepia, vintage, polaroid, escala de grises, inversión, cálido y frío.
- Nuevas herramientas de histograma, HSV y matrices de color en la ventana principal y en el editor avanzado.

### Cambiado (Changed)
- Versión del proyecto actualizada a `1.0.4`.
- Optimización de filtros para procesar imágenes mediante buffers de píxeles en lugar de llamadas pixel por pixel.
- Comparativas y previsualizaciones ejecutadas en segundo plano para mejorar la respuesta de la interfaz.
- Vista principal y editor avanzado migrados al nuevo panel de previsualización reutilizable.

### Corregido (Fixed)
- Exclusión de `.codegraph/` para evitar subir archivos locales de análisis del entorno.

---

## [1.0.3] - 2026-05-05

### Añadido (Added)
- Nuevo diseño visual de la aplicación.
- Apartado de editor avanzado para nuevas herramientas de edición.
- Filtro de ajuste RGB.

### Cambiado (Changed)
- Versión del proyecto actualizada a `1.0.3`.
- Limpieza de artefactos generados de la carpeta `build/` que no deben mantenerse en el repositorio.

---

## [1.0.2] - 2026-05-01

### Añadido (Added)
- Nueva interfaz visual con FlatLaf.
- Funcionalidad de prueba.
- Organización de filtros por categorías en el panel lateral.
- Configuración de release para generar el JAR ejecutable y el paquete standalone de Windows.

### Cambiado (Changed)
- Refactorización de la estructura principal.
- Versión del proyecto actualizada a `1.0.2`.
- Workflow de release ajustado para usar el JAR generado por Gradle sin depender de nombres `SNAPSHOT`.

### Corregido (Fixed)
- Solución de errores menores.
- Manifest del JAR configurado con la clase principal de la aplicación.

---
