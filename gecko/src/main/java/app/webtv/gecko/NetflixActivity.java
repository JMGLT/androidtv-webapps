package app.webtv.gecko;

public class NetflixActivity extends GeckoActivity {

    @Override
    protected String startUrl() {
        return "https://www.netflix.com/";
    }

    @Override
    protected String[] allowedDomains() {
        return new String[]{"netflix.com"};
    }
}
