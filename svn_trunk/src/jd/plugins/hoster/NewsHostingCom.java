package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 50772 $", interfaceVersion = 3, names = { "newshosting.com" }, urls = { "" })
public class NewsHostingCom extends UseNet {
    public NewsHostingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.newshosting.com/usenet-access-plans.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www.newshosting.com/terms-of-service.php";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface NewsHostingComConfigInterface extends UsenetAccountConfigInterface {
    };

    private Form getLoginForm(Browser br) {
        Form login = br.getFormbyActionRegex("^$");
        if (login == null) {
            login = br.getFormbyActionRegex("^/customer/login.php$");
            if (login == null) {
                login = br.getFormbyActionRegex("^/customer/index.php$");
            }
        }
        return login;
    }

    private AccountInfo quickCheckAccountInfo(Account account) throws Exception {
        final AccountInfo ai = account.getAccountInfo();
        if (ai == null) {
            return null;
        } else if (!ai.isUnlimitedTraffic()) {
            /* Limitedtraffic -> Extended check required to find current traffic-left value */
            return null;
        } else if (account.getStringProperty(USENET_USERNAME, null) == null) {
            /* No usenet username available -> Quick check impossible */
            return null;
        }
        try {
            verifyUseNetLogins(account);
            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
            ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
            return ai;
        } catch (InvalidAuthException e2) {
            account.removeProperty(USENET_USERNAME);
            logger.log(e2);
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        synchronized (account) {
            AccountInfo ai = quickCheckAccountInfo(account);
            if (ai != null) {
                return ai;
            } else {
                ai = new AccountInfo();
            }
            ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
            br.setFollowRedirects(true);
            final Cookies cookies = account.loadCookies("");
            try {
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.getPage("https://controlpanel.newshosting.com/customer/index.php");
                    if (br.containsHTML("There have been too many attempts, please try again later")) {
                        throw new AccountUnavailableException("There have been too many attempts, please try again later", 60 * 60 * 1000l);
                    }
                    final Form login = getLoginForm(br);
                    if (login != null && login.containsHTML("username") && login.containsHTML("password")) {
                        br.getCookies(getHost()).clear();
                    } else if (br.getCookie(getHost(), "sessionID", Cookies.NOTDELETEDPATTERN) == null) {
                        br.getCookies(getHost()).clear();
                    }
                }
                if (br.getCookie(getHost(), "sessionID", Cookies.NOTDELETEDPATTERN) == null) {
                    account.clearCookies("");
                    final String userName = account.getUser();
                    br.getPage("https://controlpanel.newshosting.com/customer/index.php");
                    if (br.containsHTML("There have been too many attempts, please try again later")) {
                        throw new AccountUnavailableException("There have been too many attempts, please try again later", 60 * 60 * 1000l);
                    }
                    Form login = getLoginForm(br);
                    if (login == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    login.put("username", Encoding.urlEncode(userName));
                    login.put("password", Encoding.urlEncode(account.getPass()));
                    if (login.containsHTML("g-recaptcha")) {
                        final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                        final String code = rc2.getToken();
                        if (StringUtils.isEmpty(code)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        login.put("g-recaptcha-response", Encoding.urlEncode(code));
                    }
                    br.submitForm(login);
                    login = getLoginForm(br);
                    if (login != null && login.containsHTML("username") && login.containsHTML("password")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (br.getCookie(getHost(), "sessionID", Cookies.NOTDELETEDPATTERN) == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
                final String userName = br.getRegex(">\\s*Username\\s*</div>\\s*<div[^>]+>\\s*(.*?)\\s*<").getMatch(0);
                // final String customerID = br.getRegex("(?:Customer|User)\\s*ID\\s*:\\s*(?:</strong>)?\\s*(\\d+)").getMatch(0);
                if (StringUtils.isEmpty(userName)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    account.setProperty(USENET_USERNAME, userName.trim());
                }
                final String nntpStatus = br.getRegex(">\\s*NNTP Service\\s*</div>\\s*<div[^>]+>\\s*(.*?)\\s*<").getMatch(0);
                if (!StringUtils.equalsIgnoreCase(nntpStatus, "active")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "NNTP Status:" + nntpStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final String validUntil = br.getRegex("Next Bill:</strong>\\s*(.*?)<").getMatch(0);
                final String connectionsString = br.getRegex(">\\s*Connections\\s*</div>\\s*<div[^>]+>\\s*(\\d+)\\s*<").getMatch(0);
                final String bucketType = br.getRegex(">\\s*Your Plan\\s*</div>\\s*<div[^>]+>\\s*(.*?)\\s*<").getMatch(0);
                int connections = 1;
                if (bucketType != null) {
                    ai.setStatus(Encoding.htmlOnlyDecode(bucketType));
                    if (StringUtils.containsIgnoreCase(bucketType, "lite")) {
                        connections = 30;
                    } else if (StringUtils.containsIgnoreCase(bucketType, "Unlimited")) {
                        connections = 30;
                    } else if (StringUtils.containsIgnoreCase(bucketType, "Powerpack")) {
                        connections = 60;
                    } else {
                        // smallest number of connections
                        connections = 5;
                    }
                } else {
                    ai.setStatus("Unknown Type");
                }
                if (connectionsString != null) {
                    connections = Integer.parseInt(connectionsString);
                }
                account.setMaxSimultanDownloads(connections);
                if (validUntil != null) {
                    final long date = TimeFormatter.getMilliSeconds(validUntil, "MMM dd',' yyyy", null);
                    if (date > 0) {
                        ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                    }
                }
                // TODO
                final String trafficTotal = br.getRegex("Byte Allott?ment:</strong>\\s*(\\d+)").getMatch(0);
                final String trafficLeft = br.getRegex("Bytes Remaining:</strong>\\s*(.*?)<").getMatch(0);
                if (trafficLeft != null && trafficTotal != null) {
                    ai.setTrafficMax(Long.parseLong(trafficTotal));
                    ai.setTrafficLeft(trafficLeft);
                } else if (StringUtils.equalsIgnoreCase(trafficLeft, "unlimited") || StringUtils.equalsIgnoreCase(trafficLeft, "Powerpack")) {
                    ai.setUnlimitedTraffic();
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } catch (IOException e) {
                logger.log(e);
                try {
                    verifyUseNetLogins(account);
                    account.setRefreshTimeout(5 * 60 * 60 * 1000l);
                    return ai;
                } catch (InvalidAuthException e2) {
                    if (account.getProperty(USENET_USERNAME) != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, null, PluginException.VALUE_ID_PREMIUM_DISABLE, e2);
                    } else {
                        Exceptions.addSuppressed(e, e2);
                        throw e;
                    }
                }
            }
            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
            return ai;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        for (final String server : new String[] { "news.newshosting.com", "news-us.newshosting.com", "news-eu.newshosting.com", "news-nl.newshosting.com", "news-de.newshosting.com" }) {
            ret.addAll(UsenetServer.createServerList(server, false, 119, 23, 25, 80, 3128));
            ret.addAll(UsenetServer.createServerList(server, true, 563, 443));
        }
        return ret;
    }
}
