package jd.plugins.hoster;

import java.util.List;
import java.util.Locale;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 41665 $", interfaceVersion = 3, names = { "get24.org" }, urls = { "" })
public class GeT24Org extends PluginForHost {
    private static final String VERSION = "0.0.1";

    public GeT24Org(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/pricing");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Jdownloader " + VERSION);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        String response = br.postPage("https://" + getHost() + "/api/login", "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + Hash.getSHA256(account.getPass()));
        if (!Boolean.parseBoolean(PluginJSonUtils.getJson(response, "ok")) && StringUtils.equalsIgnoreCase(PluginJSonUtils.getJson(response, "reason"), "invalid credentials")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong login or password", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        Long date_expire = TimeFormatter.getMilliSeconds(PluginJSonUtils.getJson(response, "date_expire"), "yyyy-MM-dd", Locale.ENGLISH);
        ai.setValidUntil(date_expire);
        long transfer_left = (long) (Float.parseFloat(PluginJSonUtils.getJson(response, "transfer_left")) * 1024 * 1024 * 1024);
        ai.setTrafficLeft(transfer_left);
        long transfer_max = (long) (Float.parseFloat(PluginJSonUtils.getJson(response, "transfer_max")) * 1024 * 1024 * 1024);
        ai.setTrafficMax(transfer_max);
        account.setMaxSimultanDownloads(3);
        account.setConcurrentUsePossible(true);
        // hosts list
        account.setType(AccountType.PREMIUM);
        response = br.getPage("https://" + getHost() + "/api/hosts/enabled");
        final List<String> supportedHosts = (List) JavaScriptEngineFactory.jsonToJavaObject(response);
        ai.setMultiHostSupport(this, supportedHosts);
        // acc_info.setStatus("Premium User");
        return ai;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = this.getHost() + "directurl";
        String directurl = checkDirectLink(link, directurlproperty);
        if (directurl == null) {
            String post_data = "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + Hash.getSHA256(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL());
            // "&url=" + Encoding.urlEncode(link.getDownloadURL());
            String response = br.postPage("https://get24.org/api/debrid/geturl", post_data); // security
            if (!Boolean.parseBoolean(PluginJSonUtils.getJson(response, "ok"))) {
                String reason = PluginJSonUtils.getJson(response, "reason");
                if (reason.equals("invalid credentials")) {
                    /* Permanently disable account */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong login or password");
                } else if (reason.equals("user not activated")) {
                    /* Permanently disable account */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Email not activated");
                } else if (reason.equals("premium required")) {
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "premium required", 5 * 60 * 1000l);
                } else if (reason.equals("premiumX required")) {
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "premiumX required", 5 * 60 * 1000l);
                } else if (reason.equals("no transfer")) {
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "daily limit exceeded", 5 * 60 * 1000l);
                } else if (reason.equals("host daily limit exceeded")) {
                    // TODO: allow launching other
                    // links/hosts
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "host daily limit exceeded", 5 * 60 * 1000l);
                } else if (reason.equals("file removed")) {
                    /*
                     * NEVER trust multihoster offline status --> Try next download candidate instead. If the URL really is offline, the
                     * real plugin should find the correct status via the next availablecheck!
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File offline according to multihoster", 5 * 60 * 1000l);
                } else if (reason.equals("host not supported")) {
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "host not supported", 5 * 60 * 1000l);
                } else if (reason.equals("host disabled")) {
                    /* Skip to next download candidate */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "host disabled", 5 * 60 * 1000l);
                } else if (reason.equals("temporary error")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "temporary error", 5 * 60 * 1000l); // we can try
                    // another links
                    // or
                    // hosts
                    // probably
                } else if (reason.equals("unknown error")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "unknown error", 5 * 60 * 1000l); // we can try
                    // another
                    // hosts or maybe
                    // even links
                } else {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "unknown error");
                }
            }
            directurl = PluginJSonUtils.getJson(response, "url");
            if (StringUtils.isEmpty(directurl)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "unknown error failed to find final downloadurl", 5 * 60 * 1000l);
            }
        }
        // TODO: resume support
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, directurl, false, 1);
        link.setProperty(directurlproperty, directurl);
        // TODO: validate status code
        dl.startDownload();
        // link_hash
        // url
        // filename
        // filesize
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    link.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new AccountRequiredException();
    }
}
