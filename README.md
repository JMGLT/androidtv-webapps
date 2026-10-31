# androidtv-webapps

Apps mínimas para Android TV que abren servicios web (Netflix, Prime Video) a pantalla completa y se manejan con el mando. Sin analíticas ni servicios de terceros.

## Módulos

| Módulo | Motor | Aplicación |
|---|---|---|
| `webview` | WebView del sistema | `app.webtv.webview` |

## Compilación

Cada push a `main` ejecuta GitHub Actions, que compila los APK de depuración y los publica, junto con `build.log` y `status.txt`, en la rama `builds`.

## Instalación

```
adb connect IP_DE_LA_BOX:5555
adb install -r webview-debug.apk
```

## Manejo con el mando

- Flechas: mover el cursor (en los bordes, desplazan la página).
- OK: clic.
- Menú: alternar entre modo cursor y navegación por foco.
- Play/Pausa: espacio (pausa/reanuda).
- Adelantar/Rebobinar: flecha derecha/izquierda del reproductor.
- Atrás: volver, o salir de la pantalla completa.

## Notas

- La reproducción con DRM usa el Widevine del dispositivo. Con L3, la calidad máxima suele ser HD básico (hasta 720p); no se puede garantizar según el servicio.
- Solo se permite navegar a los dominios de cada servicio, y el permiso de medios protegidos se concede únicamente a esos dominios.
- Requiere un WebView reciente; si el servicio pide actualizar el navegador, actualiza *Android System WebView*.
