package app.webtv.webview;

public class DaznActivity extends WebActivity {

    @Override
    protected String startUrl() {
        return "https://www.dazn.com/it-IT";
    }

    @Override
    protected String[] allowedDomains() {
        return new String[]{"dazn.com"};
    }

    @Override
    protected String languageTag() {
        return "it-IT";
    }

    @Override
    protected boolean reduceMotion() {
        return true;
    }
}
