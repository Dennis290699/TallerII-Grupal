# Registro de Cambios (Changelog)

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto se adhiere a [Versionado Semántico](https://semver.org/lang/es/).

## [Unreleased] (No publicado aún)

Aquí puedes ir anotando los cambios que estás haciendo y que aún no has lanzado en una nueva versión.

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
