package app.webtv.webview;

import android.annotation.SuppressLint;
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
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abre una web a pantalla completa en un WebView, con navegación por cursor
 * controlada con el mando. No incluye analíticas ni ningún servicio de terceros.
 */
public abstract class WebActivity extends Activity {

    protected abstract String startUrl();

    /** Dominios a los que se permite navegar y para los que se concede DRM. */
    protected abstract String[] allowedDomains();

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;

    private FrameLayout root;
    private WebView web;
    private CursorView cursorView;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;

    private String rejectScript = "";
    private boolean cursorMode = true;
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

        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(MATCH, MATCH));

        cursorView = new CursorView(this, density);
        root.addView(cursorView, new FrameLayout.LayoutParams(MATCH, MATCH));

        setContentView(root);
        hideSystemUi();
        rejectScript = readAsset("reject_cookies.js");
        configureWebView();

        root.post(() -> {
            cx = root.getWidth() / 2f;
            cy = root.getHeight() / 2f;
            cursorView.moveTo(cx, cy);
        });

        Toast.makeText(this, R.string.help_toast, Toast.LENGTH_LONG).show();
        web.loadUrl(startUrl());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        // Modo escritorio: se cambia solo la parte del sistema operativo del user agent;
        // la versión real del motor se mantiene.
        String ua = s.getUserAgentString()
                .replaceAll("\\(Linux; Android[^)]*\\)", "(Windows NT 10.0; Win64; x64)")
                .replace(" Version/4.0", "")
                .replace(" Mobile", "");
        s.setUserAgentString(ua);

        // Cookies de terceros rechazadas por defecto; las propias del servicio se aceptan
        // para poder mantener la sesión iniciada.
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                // true = bloquear la navegación
                boolean block = !("https".equals(u.getScheme()) && hostAllowed(u.getHost()));
                if (block) {
                    Log.w("webtv", "Navegación bloqueada: " + u);
                }
                return block;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Rechaza automáticamente el aviso de cookies de la página.
                if (!rejectScript.isEmpty() && url != null
                        && hostAllowed(Uri.parse(url).getHost())) {
                    view.evaluateJavascript(rejectScript, null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    Uri origin = request.getOrigin();
                    String host = origin == null ? null : origin.getHost();
                    List<String> granted = new ArrayList<>();
                    if (hostAllowed(host)) {
                        for (String r : request.getResources()) {
                            // Solo medios protegidos (DRM / Widevine); nada de cámara ni micrófono.
                            if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) {
                                granted.add(r);
                            }
                        }
                    }
                    if (granted.isEmpty()) {
                        request.deny();
                    } else {
                        request.grant(granted.toArray(new String[0]));
                    }
                });
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                enterCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                exitCustomView();
            }
        });
    }

    private String readAsset(String name) {
        try (InputStream in = getAssets().open(name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        } catch (IOException e) {
            return "";
        }
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

    // ---- Pantalla completa del vídeo ----

    private void enterCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customView = view;
        customCallback = callback;
        web.setVisibility(View.GONE);
        root.addView(view, 0, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    private void exitCustomView() {
        if (customView == null) {
            return;
        }
        root.removeView(customView);
        customView = null;
        web.setVisibility(View.VISIBLE);
        WebChromeClient.CustomViewCallback cb = customCallback;
        customCallback = null;
        if (cb != null) {
            cb.onCustomViewHidden();
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitCustomView();
        } else if (web.canGoBack()) {
            web.goBack();
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

    private View target() {
        return customView != null ? customView : web;
    }

    private void moveCursor(int dx, int dy, int repeat) {
        float step = (14f + Math.min(repeat, 25) * 2f) * density;
        cx = clamp(cx + dx * step, 0, root.getWidth() - 1);
        cy = clamp(cy + dy * step, 0, root.getHeight() - 1);

        // Si el cursor está en el borde y se sigue empujando, se desplaza la página.
        float edge = 4f * density;
        if (customView == null) {
            if (dy < 0 && cy <= edge) {
                web.scrollBy(0, (int) (-step * 2));
            } else if (dy > 0 && cy >= root.getHeight() - 1 - edge) {
                web.scrollBy(0, (int) (step * 2));
            } else if (dx < 0 && cx <= edge) {
                web.scrollBy((int) (-step * 2), 0);
            } else if (dx > 0 && cx >= root.getWidth() - 1 - edge) {
                web.scrollBy((int) (step * 2), 0);
            }
        }
        cursorView.moveTo(cx, cy);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void clickAtCursor() {
        View t = target();
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cx, cy, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        t.dispatchTouchEvent(down);
        down.recycle();
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, cx, cy, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        t.dispatchTouchEvent(up);
        up.recycle();
    }

    private void sendKey(int keyCode) {
        View t = target();
        long now = SystemClock.uptimeMillis();
        t.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        t.dispatchKeyEvent(new KeyEvent(now, now + 20, KeyEvent.ACTION_UP, keyCode, 0));
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
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        root.removeAllViews();
        web.destroy();
        super.onDestroy();
    }

    /** Punto que hace de cursor, dibujado encima del WebView. */
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
