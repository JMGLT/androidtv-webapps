package app.webtv.webview;

public class PrimeActivity extends WebActivity {

    @Override
    protected String startUrl() {
        return "https://www.primevideo.com/";
    }

    @Override
    protected String[] allowedDomains() {
        // El inicio de sesión de Prime Video pasa por dominios de Amazon.
        return new String[]{
                "primevideo.com",
                "amazon.es", "amazon.com", "amazon.co.uk",
                "amazon.de", "amazon.fr", "amazon.it"
        };
    }
}
