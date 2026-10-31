package app.webtv.gecko;

public class DaznActivity extends GeckoActivity {

    @Override
    protected String startUrl() {
        return "https://www.dazn.com/it-IT";
    }

    @Override
    protected String[] allowedDomains() {
        return new String[]{"dazn.com"};
    }
}
