package app.webtv.webview;

public class NetflixActivity extends WebActivity {

    @Override
    protected String startUrl() {
        return "https://www.netflix.com/";
    }

    @Override
    protected String[] allowedDomains() {
        return new String[]{"netflix.com"};
    }
}
