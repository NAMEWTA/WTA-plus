import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;

public class WtaT40UaProbe {
    public static void main(String[] args) {
        UserAgent absent = UserAgentUtil.parse(null);
        UserAgent chrome = UserAgentUtil.parse("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        System.out.println("absent_is_null=" + (absent == null));
        System.out.println("chrome_browser=" + chrome.getBrowser().getName());
        System.out.println("chrome_os=" + chrome.getOs().getName());
        try {
            absent.getBrowser().getName();
            System.out.println("absent_dereference=unexpected_success");
        } catch (NullPointerException ignored) {
            System.out.println("absent_dereference=NullPointerException");
        }
    }
}
