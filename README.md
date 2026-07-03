# 🎵 VolumeSkip

**Control musical al estilo Motorola** — Adelanta y retrocede canciones con los botones de volumen.

Mantén presionado **Vol+** para la siguiente canción y **Vol-** para la canción anterior. Las presiones cortas siguen controlando el volumen normal.

---

## ✨ Características

- ✅ **Presión corta** → Volumen normal (sube/baja)
- ⏭ **Mantener Vol+** → Siguiente canción
- ⏮ **Mantener Vol-** → Canción anterior
- 🔒 **Funciona con pantalla apagada** y en segundo plano
- 📱 **Compatible** desde Android 8.0 (API 26) en adelante
- 🎯 **Controla cualquier app de música** (Spotify, YouTube Music, etc.)
- ⚡ **Sin root** ni permisos especiales

---

## 📲 Instalación

1. Descarga el APK desde **[Releases](https://github.com/MichaelARC-NI/VolumeSkip/releases)**
2. Ábrelo desde el gestor de archivos
3. Concede permisos de instalación si es necesario
4. Toca **Instalar**

### Por ADB

```bash
adb install VolumeSkip.apk
```

---

## 🚀 Cómo usar

1. Abre la app **VolumeSkip**
2. Presiona **⚙ ACTIVAR ACCESIBILIDAD** → te lleva a Ajustes
3. Busca **VolumeSkip** y actívalo en Accesibilidad
4. Presiona **▶ ACTIVAR SERVICIO** para la notificación persistente
5. ¡Listo! Pon música y prueba:

| Acción | Resultado |
|--------|-----------|
| Presión corta Vol+ | Sube volumen |
| Presión corta Vol- | Baja volumen |
| Mantener Vol+ (400ms) | ⏭ Siguiente canción |
| Mantener Vol- (400ms) | ⏮ Canción anterior |

> **Nota:** En algunos dispositivos (HyperOS, MIUI, ColorOS) la accesibilidad podría tener restricciones adicionales. Actívala manualmente desde *Ajustes > Accesibilidad > VolumeSkip*.

---

## 🛠 Compilar desde el código

```bash
git clone https://github.com/MichaelARC-NI/VolumeSkip.git
cd VolumeSkip
./gradlew assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/`.

### Requisitos

- JDK 17
- Android SDK 34
- Gradle 8.10.2

---

## 📋 Notas técnicas

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Lenguaje**: Kotlin
- **Arquitectura**: Accessibility Service + Foreground Service + AudioManager
- **Package**: `com.volumeskip`

---

## 📱 Contacto

**Desarrollador:** Michael Antonio Rodríguez Condega

- **Facebook:** [Michael Antonio Rodriguez Condega](https://www.facebook.com/share/1D1pfVdbXE/)
- **Telegram:** [@Michael_Antonio_Rodriguez](https://t.me/Michael_Antonio_Rodriguez)
- **WhatsApp:** [Escribirme](https://wa.me/50583341349?text=Hola%20Michael)
- **YouTube:** [@androidmovil](https://youtube.com/@androidmovil?si=dqzoWBDy1EsNaM7v)
- **Email:** androidmovil@proton.me

---

## 📜 Licencia

MIT © 2026 Michael Antonio Rodríguez Condega
