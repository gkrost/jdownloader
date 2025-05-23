//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.text.TextDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 50857 $", interfaceVersion = 3, names = {}, urls = {})
public class LiteroticaCom extends PluginForHost {
    public LiteroticaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.literotica.com/stories/tos.php";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "literotica.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/s/([a-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("/c/[^\"]+\"[^>]*>\\s*([^<>\"]+)\\s*</a>.*?>\\s*([^<>\"]+)\\s*</(div|span)>");
        String author = finfo.getMatch(0);
        String filename = finfo.getMatch(1);
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = this.getFID(link);
        }
        if (author != null) {
            author = Encoding.htmlDecode(author).trim();
            filename = Encoding.htmlDecode(filename).trim();
            filename = author + " - " + filename;
        }
        link.setFinalFileName(filename + ".html");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link);
    }

    private void doFree(final DownloadLink link) throws Exception, PluginException {
        final String contentID = br.getRegex("(\"|)favorite_count\\1\\s*:\\s*\\d+\\s*,\\s*\\1id\\1\\s*:\\s*(\\d+)").getMatch(1);
        if (contentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Collect text content of all pages if multiple pages are available. */
        final StringBuilder sb = new StringBuilder();
        int pageCounter = 0;
        int maxPages = -1;
        do {
            pageCounter++;
            logger.info("Crawling page: " + pageCounter + "/" + maxPages);
            br.getPage("https://" + this.getHost() + "/api/3/stories/" + contentID + "?params=%7B%22contentPage%22%3A" + pageCounter + "%7D");
            if (br.getHttpConnection().getResponseCode() == 404) {
                if (pageCounter == 1) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
                }
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            if (maxPages == -1) {
                maxPages = ((Number) JavaScriptEngineFactory.walkJson(entries, "meta/pages_count")).intValue();
            }
            final String text = (String) entries.get("pageText");
            if (StringUtils.isEmpty(text)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Add page marker */
            sb.append("<br  />");
            sb.append("***** Page " + pageCounter + " *****");
            sb.append("<br  />");
            /* Use HTML linebreaks. */
            sb.append(text.replace("\r\n", "<br  />"));
            if (maxPages == -1) {
                logger.info("Stopping because: Failed to find maxPages");
                break;
            } else if (pageCounter >= maxPages) {
                logger.info("Stopping because: Reached end");
                break;
            }
        } while (!this.isAbort());
        dl = new TextDownloader(this, link, sb.toString());
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}