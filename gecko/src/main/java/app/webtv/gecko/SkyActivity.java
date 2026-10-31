package app.webtv.gecko;

public class SkyActivity extends GeckoActivity {

    @Override
    protected String startUrl() {
        return "https://www.sky.it/login";
    }

    @Override
    protected String[] allowedDomains() {
        // Incluye el acceso con Sky ID y el reproductor (todos bajo sky.it).
        return new String[]{"sky.it"};
    }

    @Override
    protected String languageTag() {
        return "it-IT";
    }
}
