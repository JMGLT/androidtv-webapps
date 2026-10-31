# androidtv-webapps

Apps mínimas para Android TV que abren servicios web a pantalla completa y se manejan con el mando. Sin analíticas ni servicios de terceros. Rechazan las cookies de terceros y pulsan «Rechazar» en los avisos de cookies.

## Servicios y motores

| Servicio | WebView (`*_webapp_*`) | GeckoView (`*_geckoapp_*`) |
|---|---|---|
| Netflix | `app.webtv.webview.netflix` | `app.webtv.gecko.netflix` |
| Prime Video | `app.webtv.webview.prime` | `app.webtv.gecko.prime` |
| DAZN | `app.webtv.webview.dazn` | `app.webtv.gecko.dazn` |
| Sky (Italia, `sky.it/login`) | `app.webtv.webview.sky` | `app.webtv.gecko.sky` |

- **WebView**: ligera, usa el WebView del sistema (conviene tenerlo actualizado).
- **GeckoView**: lleva dentro el motor de Firefox (APK de unos 100 MB, uno por arquitectura).

## Versiones y releases

Cada app tiene su propia versión (MAYOR.MENOR.PARCHE) en `gradle.properties`, con la forma `version.<motor>.<servicio>`. **Se sube solo la de las apps que cambian.** Cada push a `main` compila todo, y publica una release por app y versión que todavía no exista:

- `Servicio_webapp-vX.Y.Z` con `Servicio_webapp_X.Y.Z.apk` (WebView)
- `Servicio_geckoapp-vX.Y.Z` con `Servicio_geckoapp_X.Y.Z_armeabi-v7a.apk` (boxes de 32 bits) y `..._arm64-v8a.apk` (64 bits)

Si una versión ya tiene release, se conserva tal cual (hay que subir la versión de esa app para publicar un cambio). La rama `builds` guarda además el registro de la última compilación.

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
