package app.webtv.gecko;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.ScreenLength;

/**
 * Abre una web a pantalla completa con GeckoView (motor de Firefox) y navegación
 * por cursor con el mando. No incluye analíticas ni informes de fallos.
 */
public abstract class GeckoActivity extends Activity {

    protected abstract String startUrl();

    /** Dominios a los que se permite navegar y para los que se concede DRM. */
    protected abstract String[] allowedDomains();

    /** Idioma preferido de la web (etiqueta BCP 47, p. ej. "it-IT"); null = el del sistema. */
    protected String languageTag() {
        return null;
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    // GeckoRuntime solo puede existir una vez por proceso.
    private static GeckoRuntime runtime;

    private FrameLayout root;
    private GeckoView geckoView;
    private GeckoSession session;
    private CursorView cursorView;

    private boolean cursorMode = true;
    private boolean canGoBack;
    private boolean fullScreen;
    private float cx;
    private float cy;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        density = getResources().getDisplayMetrics().density;

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        geckoView = new GeckoView(this);
        root.addView(geckoView, new FrameLayout.LayoutParams(MATCH, MATCH));

        cursorView = new CursorView(this, density);
        root.addView(cursorView, new FrameLayout.LayoutParams(MATCH, MATCH));

        setContentView(root);
        hideSystemUi();

        if (runtime == null) {
            GeckoRuntimeSettings.Builder runtimeSettings = new GeckoRuntimeSettings.Builder();
            if (languageTag() != null) {
                runtimeSettings.locales(new String[]{languageTag()});
            }
            runtime = GeckoRuntime.create(getApplicationContext(), runtimeSettings.build());
            // Cookies de terceros rechazadas por defecto; las propias del servicio se aceptan
            // para poder mantener la sesión iniciada.
            runtime.getSettings().getContentBlocking()
                    .setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY);
            // Extensión integrada que rechaza automáticamente los avisos de cookies.
            runtime.getWebExtensionController().ensureBuiltIn(
                    "resource://android/assets/cookie-reject/",
                    "cookie-reject@androidtv-webapps");
        }

        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(false)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)
                .build();
        session = new GeckoSession(settings);

        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(
                    GeckoSession s, ContentPermission perm) {
                Uri uri = perm.uri == null ? null : Uri.parse(perm.uri);
                boolean trusted = uri != null && hostAllowed(uri.getHost());
                boolean wanted = perm.permission == PERMISSION_MEDIA_KEY_SYSTEM_ACCESS
                        || perm.permission == PERMISSION_AUTOPLAY_AUDIBLE
                        || perm.permission == PERMISSION_AUTOPLAY_INAUDIBLE;
                return GeckoResult.fromValue(trusted && wanted
                        ? ContentPermission.VALUE_ALLOW
                        : ContentPermission.VALUE_DENY);
            }
        });

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(
                    GeckoSession s, LoadRequest request) {
                Uri u = Uri.parse(request.uri);
                boolean ok = "https".equals(u.getScheme()) && hostAllowed(u.getHost());
                if (!ok) {
                    Log.w("webtv", "Navegación bloqueada: " + u);
                }
                return GeckoResult.fromValue(ok ? AllowOrDeny.ALLOW : AllowOrDeny.DENY);
            }

            @Override
            public void onCanGoBack(GeckoSession s, boolean value) {
                canGoBack = value;
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession s, boolean value) {
                fullScreen = value;
                hideSystemUi();
            }
        });

        session.open(runtime);
        geckoView.setSession(session);

        root.post(() -> {
            cx = root.getWidth() / 2f;
            cy = root.getHeight() / 2f;
            cursorView.moveTo(cx, cy);
        });

        Toast.makeText(this, R.string.help_toast, Toast.LENGTH_LONG).show();
        session.loadUri(startUrl());
    }

    private boolean hostAllowed(String host) {
        if (host == null) {
            return false;
        }
        for (String d : allowedDomains()) {
            if (host.equals(d) || host.endsWith("." + d)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (fullScreen) {
            session.exitFullScreen();
        } else if (canGoBack) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ---- Mando a distancia ----

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        final int code = e.getKeyCode();
        final boolean down = e.getAction() == KeyEvent.ACTION_DOWN;

        if (code == KeyEvent.KEYCODE_MENU) {
            if (down && e.getRepeatCount() == 0) {
                cursorMode = !cursorMode;
                cursorView.setActive(cursorMode);
            }
            return true;
        }

        switch (code) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (down && e.getRepeatCount() == 0) {
                    sendKey(KeyEvent.KEYCODE_SPACE);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (down) {
                    sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (down) {
                    sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
                }
                return true;
            default:
                break;
        }

        if (cursorMode) {
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (down) {
                        moveCursor(0, -1, e.getRepeatCount());
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (down) {
                        moveCursor(0, 1, e.getRepeatCount());
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (down) {
                        moveCursor(-1, 0, e.getRepeatCount());
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (down) {
                        moveCursor(1, 0, e.getRepeatCount());
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (down && e.getRepeatCount() == 0) {
                        clickAtCursor();
                    }
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    private void moveCursor(int dx, int dy, int repeat) {
        float step = (14f + Math.min(repeat, 25) * 2f) * density;
        cx = clamp(cx + dx * step, 0, root.getWidth() - 1);
        cy = clamp(cy + dy * step, 0, root.getHeight() - 1);

        // Si el cursor está en el borde y se sigue empujando, se desplaza la página.
        float edge = 4f * density;
        double scroll = step * 2;
        if (dy < 0 && cy <= edge) {
            scrollBy(0, -scroll);
        } else if (dy > 0 && cy >= root.getHeight() - 1 - edge) {
            scrollBy(0, scroll);
        } else if (dx < 0 && cx <= edge) {
            scrollBy(-scroll, 0);
        } else if (dx > 0 && cx >= root.getWidth() - 1 - edge) {
            scrollBy(scroll, 0);
        }

        cursorView.moveTo(cx, cy);
        hoverAtCursor();
    }

    private void scrollBy(double x, double y) {
        geckoView.getPanZoomController().scrollBy(
                ScreenLength.fromPixels(x), ScreenLength.fromPixels(y));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Mueve el puntero "de ratón" para que funcionen los efectos al pasar por encima. */
    private void hoverAtCursor() {
        long now = SystemClock.uptimeMillis();
        MotionEvent m = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE, cx, cy, 0);
        m.setSource(InputDevice.SOURCE_MOUSE);
        geckoView.dispatchGenericMotionEvent(m);
        m.recycle();
    }

    private void clickAtCursor() {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cx, cy, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        geckoView.dispatchTouchEvent(down);
        down.recycle();
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, cx, cy, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        geckoView.dispatchTouchEvent(up);
        up.recycle();
    }

    private void sendKey(int keyCode) {
        long now = SystemClock.uptimeMillis();
        geckoView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        geckoView.dispatchKeyEvent(new KeyEvent(now, now + 20, KeyEvent.ACTION_UP, keyCode, 0));
    }

    // ---- Ciclo de vida ----

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
        }
        super.onDestroy();
    }

    /** Punto que hace de cursor, dibujado encima de GeckoView. */
    private static final class CursorView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        private float x;
        private float y;
        private boolean active = true;

        CursorView(Context context, float density) {
            super(context);
            radius = 9f * density;
            fill.setColor(Color.argb(210, 255, 255, 255));
            ring.setColor(Color.BLACK);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(2f * density);
            setFocusable(false);
            setClickable(false);
        }

        void moveTo(float newX, float newY) {
            x = newX;
            y = newY;
            invalidate();
        }

        void setActive(boolean value) {
            active = value;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!active) {
                return;
            }
            canvas.drawCircle(x, y, radius, fill);
            canvas.drawCircle(x, y, radius, ring);
        }
    }
}
