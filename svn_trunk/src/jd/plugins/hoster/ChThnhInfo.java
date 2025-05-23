//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 49682 $", interfaceVersion = 2, names = { "chauthanh.info" }, urls = { "https?://(www\\.)?chauthanh\\.info/(animeDownload/(download|new)/\\d+/[^<>\"/]+/[^<>\"/]+|animeOST/download/[a-z0-9\\-_]+/[a-z0-9\\-_\\.]+|[a-z]+/download/[^<>\"]+\\.html)" })
public class ChThnhInfo extends antiDDoSForHost {
    public String dllink = null;

    public ChThnhInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://chauthanh.info/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static final String TYPE1 = "(?i)https?://(www\\.)?chauthanh\\.info/animeDownload/(download|new)/\\d+/[^<>\"/]+/[^<>\"/]+";
    private static final String TYPE2 = "(?i)https?://(www\\.)?chauthanh\\.info/animeOST/download/[a-z0-9\\-_]+/[a-z0-9\\-_\\.]+";
    private static final String TYPE3 = "(?i)https?://(www\\.)?chauthanh\\.info/[a-z]+/download/[^<>\"]+\\.html";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        if (link.getDownloadURL().matches(TYPE1)) {
            if (br.containsHTML("(This video does not exist|>Removed due to licensed<)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>Downloading file(.*?)\\- Download Anime").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div id=\"content_text\"><p><center><b>(.*?)</b>").getMatch(0);
            }
            dllink = br.getRegex("<p><a href=\"(/[^\"]+)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<p><a href=\"(/animeDownload/.+/download/\\d+/[^\"]+)").getMatch(0);
            }
        } else if (link.getDownloadURL().matches(TYPE2)) {
            if (!br.containsHTML("\\[Download to your computer\\]<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("File name: <b>([^<>\"]*?)</b>").getMatch(0);
            dllink = br.getRegex("\"(/animeOST/download/[^<>\"]*?)\"").getMatch(0);
        } else {
            filename = br.getRegex(">Download file ([^<>\"]*?)</h3>").getMatch(0);
            dllink = br.getRegex("class=\"p100 center middle\">\\s*<a href=\"\\.\\.(/[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                dllink = "http://chauthanh.info/anime/download" + dllink;
            }
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!dllink.contains("chauthanh.info") && dllink.startsWith("/")) {
            dllink = "http://chauthanh.info" + dllink;
        }
        link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setReadTimeout(3 * 60 * 60 * 1000);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // for the decrypter, so we have only one session of antiddos
    public void getPage(final String url) throws Exception {
        super.getPage(url);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}