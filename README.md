# androidtv-webapps

Apps mínimas para Android TV que abren servicios web a pantalla completa y se manejan con el mando. Sin analíticas ni servicios de terceros. Rechazan las cookies de terceros y pulsan «Rechazar» en los avisos de cookies.

## Servicios y motores

| Servicio | WebView (`*_webapp_*`) | GeckoView (`*_geckoapp_*`) |
|---|---|---|
| Netflix | `app.webtv.webview.netflix` | `app.webtv.gecko.netflix` |
| Prime Video | `app.webtv.webview.prime` | `app.webtv.gecko.prime` |
| DAZN | `app.webtv.webview.dazn` | `app.webtv.gecko.dazn` |

- **WebView**: ligera, usa el WebView del sistema (conviene tenerlo actualizado).
- **GeckoView**: lleva dentro el motor de Firefox (APK de unos 100 MB, uno por arquitectura).

## Versiones y releases

La versión sale de una sola línea, `appVersion` en `gradle.properties` (MAYOR.MENOR.PARCHE). Hay que subirla en cada actualización o ajuste. Cada push a `main` compila y publica una release `vX.Y.Z` con los APK:

- `Servicio_webapp_X.Y.Z.apk` (WebView)
- `Servicio_geckoapp_X.Y.Z_armeabi-v7a.apk` (GeckoView, boxes de 32 bits)
- `Servicio_geckoapp_X.Y.Z_arm64-v8a.apk` (GeckoView, 64 bits)

Si se compila otra vez con la misma versión, esa release se reemplaza. La rama `builds` guarda además el registro de la última compilación.

## Instalación

```
adb connect IP_DE_LA_BOX:5555
adb install Netflix_webapp_X.Y.Z.apk
```

Cada compilación se firma con una clave de depuración distinta, así que antes de instalar una versión nueva hay que desinstalar la anterior (`adb uninstall app.webtv.webview.netflix`, etc.).

## Iconos

Están en `webview/src/main/res/drawable-nodpi/` y `gecko/src/main/res/drawable-nodpi/` (`ic_<servicio>.png` 512×512 y `banner_<servicio>.png` 640×360). Se pueden sustituir por otros con el mismo nombre.

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
