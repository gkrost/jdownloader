//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.MultiHostHost;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 50303 $", interfaceVersion = 3, names = { "multivip.net" }, urls = { "" })
public class MultiVipNet extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            APIKEY             = "jd2";
    private static final boolean                           USE_API            = true;
    /* Default value is 10 */
    private static AtomicInteger                           maxPrem            = new AtomicInteger(10);
    private static final String                            API_BASE           = "http://multivip.net/api.php";
    private static MultiHosterManagement                   mhm                = new MultiHosterManagement("multivip.net");

    public MultiVipNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://" + getHost());
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setCookie(getHost(), "lang", "en");
        return br;
    }

    @Override
    public String getAGBLink() {
        return "http://" + getHost() + "/contact.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        br.setFollowRedirects(true);
        br.setCurrentURL(null);
        int maxChunks = -2;
        link.setProperty("multivipnetdirectlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to downloadable content", 50);
        }
        dl.startDownload();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST, LazyPlugin.FEATURE.API_KEY_LOGIN };
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        showMessage(link, "Task 1: Generating Link");
        String dllink = checkDirectLink(link, "multivipnetdirectlink");
        if (dllink == null) {
            /* request Download */
            if (USE_API) {
                br.getPage(API_BASE + "?apipass=" + APIKEY + "&do=addlink&vipkey=" + Encoding.urlEncode(account.getPass()) + "&ip=&link=" + Encoding.urlEncode(link.getDownloadURL()));
                /* Should never happen because size limit is set in fetchAccountInfo and handled via canHandle */
                if ("204".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                    /*
                     * Should never happen because size limit is set in fetchAccountInfo and handled via canHandle. Update 16.05.2015: This
                     * can indeed happen for links with unknown filesize!
                     */
                    account.setType(AccountType.FREE);
                    account.getAccountInfo().setStatus("Free account");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link too big to download via " + this.getHost());
                }
                dllink = PluginJSonUtils.getJsonValue(br, "directlink");
            } else {
                br.postPage("http://multivip.net/links.php", "do=addlinks&links=" + Encoding.urlEncode(link.getDownloadURL()) + "&vipkey=" + Encoding.urlEncode(account.getPass()));
                if (br.containsHTML("Universal VIP key is missing or incorrect")) {
                    logger.info("Given Vip key is invalid");
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid Vip key!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.containsHTML("This is a FREE key and File size in")) {
                    /*
                     * Should never happen because size limit is set in fetchAccountInfo and handled via canHandle. Update 16.05.2015: This
                     * can indeed happen for links with unknown filesize!
                     */
                    account.setType(AccountType.FREE);
                    account.getAccountInfo().setStatus("Free Account");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This link too big to download via " + this.getHost());
                } else if (br.containsHTML("Unfortunately this key was expired")) {
                    /* Our account has expired */
                    final String expire_date = br.getRegex("Unfortunately this key was expired <strong>([^<>\"]*?)</strong>").getMatch(0);
                    if (expire_date != null) {
                        account.getAccountInfo().setValidUntil(TimeFormatter.getMilliSeconds(expire_date, "MM-dd-yy, hh:mm", Locale.ENGLISH));
                    }
                    account.getAccountInfo().setExpired(true);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML("Failed to get information about the file")) {
                    logger.info("Seems like the current host doesn't work anymore --> Disabling it");
                    tempUnavailableHoster(account, link, 60 * 60 * 1000l);
                }
                final Regex account_info = br.getRegex("you have after this action <strong>([^<>\"]*?)</strong> till <strong>([^<>\"]*?) </strong>");
                final String traffic_left = account_info.getMatch(0);
                final String expire_date = account_info.getMatch(1);
                if (traffic_left != null && expire_date != null) {
                    account.getAccountInfo().setTrafficLeft(SizeFormatter.getSize(traffic_left));
                    account.getAccountInfo().setValidUntil(TimeFormatter.getMilliSeconds(expire_date, "MM-dd-yy, hh:mm", Locale.ENGLISH));
                }
                dllink = br.getRegex("\"(http[^<>\"]*?)\"").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed to obtain final downloadurl", 50);
            }
            dllink = dllink.replace("\\", "");
        }
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, dllink);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        account.setMaxSimultanDownloads(20);
        maxPrem.set(20);
        br.getPage(API_BASE + "?apipass=" + APIKEY + "&do=keycheck&vipkey=" + Encoding.urlEncode(account.getPass()));
        // TODO: Make use of json parser everywhere
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String error_txt = (String) entries.get("error_txt");
        if (error_txt != null) {
            throw new AccountInvalidException(error_txt);
        }
        final String expire = PluginJSonUtils.getJsonValue(br, "diedate");
        final String max_downloadable_filesize_kb = PluginJSonUtils.getJsonValue(br, "limit");
        final long max_downloadable_filesize_bytes = Long.parseLong(max_downloadable_filesize_kb) * 1024;
        final String traffic_left_kb = PluginJSonUtils.getJsonValue(br, "points");
        ai.setValidUntil(Long.parseLong(expire) * 1000);
        ai.setTrafficLeft(Long.parseLong(traffic_left_kb) * 1024);
        br.getPage("/api.php?apipass=" + APIKEY + "&do=getlist");
        final List<MultiHostHost> supportedhosts = new ArrayList<MultiHostHost>();
        final String[] hostDomains = br.getRegex("\"allow\":\\[(.*?)\\]").getColumn(0);
        for (final String domains : hostDomains) {
            final MultiHostHost mhost = new MultiHostHost();
            final String[] realDomains = new Regex(domains, "\"(.*?)\"").getColumn(0);
            for (final String realDomain : realDomains) {
                mhost.addDomain(realDomain);
            }
            if (max_downloadable_filesize_bytes > 0) {
                mhost.setTrafficLeft(max_downloadable_filesize_bytes);
                mhost.setTrafficMax(max_downloadable_filesize_bytes);
            }
            supportedhosts.add(mhost);
        }
        if (max_downloadable_filesize_kb.equals("0")) {
            /* Premium keys have no max downloadable filesize limit */
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Vip key");
        } else {
            /* Free keys have a max downloadable filesize limit */
            /*
             * Free keys have an expire date as well. Once expired, they cannot be used to download anything anymore and JD will not accept
             * them (shows correct message'Account expired').
             */
            account.setType(AccountType.FREE);
            ai.setStatus("Free Vip key");
        }
        ai.setMultiHostSupportV2(this, supportedhosts);
        return ai;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, final long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    protected String getAPILoginHelpURL() {
        return "http://" + getHost() + "/";
    }

    @Override
    protected boolean looksLikeValidAPIKey(final String str) {
        return true;
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return maxPrem.get();
    }
}