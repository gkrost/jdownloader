//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision: 51002 $", interfaceVersion = 2, names = { "file-upload.net" }, urls = { "https?://(www\\.|en\\.)?file\\-upload\\.net/((member/){0,1}download\\-\\d+/(.*?)\\.html|view\\-\\d+/(.*?)\\.html|member/view_\\d+_(.*?)\\.html|member/data3\\.php\\?user=(.*?)\\&name=(.*))" })
public class FileUploadDotnet extends antiDDoSForHost {
    private final Pattern PAT_Download = Pattern.compile("https?://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);
    private final Pattern PAT_VIEW     = Pattern.compile("https?://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);
    private final Pattern PAT_Member   = Pattern.compile("https?://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);

    public FileUploadDotnet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("https?://(en\\.)?file\\-upload", "https://www.file-upload"));
    }

    public String getAGBLink() {
        return "http://www." + getHost() + "/to-agb.html";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        br = new Browser();
        br.setFollowRedirects(true);
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                String downloadurl = downloadLink.getDownloadURL();
                getPage(downloadurl);
                if (!br.containsHTML(">Datei existiert nicht")) {
                    // Get complete name
                    String filename = br.getRegex("<title>File\\-Upload\\.net \\- ([^<>\"]*?)</title>").getMatch(0);
                    // This name might be cut
                    if (filename == null) {
                        filename = br.getRegex("<h1 class=\\'dateiname\\'>([^<>\"]*?)</h1>").getMatch(0);
                    }
                    String filesize = br.getRegex("label>Dateigröße:</label><span>([^<>\"]+)").getMatch(0);
                    if (filesize == null) {
                        filesize = br.getRegex("(\\d+(\\.\\d+)? ?(B(ytes)?|KB|MB|GB))").getMatch(0);
                    }
                    if (filesize != null) {
                        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                    downloadLink.setName(Encoding.htmlDecode(filename));
                    return AvailableStatus.TRUE;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
                /* LinkCheck für DownloadFiles */
                String downloadurl = downloadLink.getDownloadURL();
                getPage(downloadurl);
                if (!br.getURL().contains("view")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b>\\s*(.*?)\\s*Kbyte").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return AvailableStatus.TRUE;
                }
            }
        } catch (Exception e) {
            logger.log(e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @SuppressWarnings("deprecation")
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (new Regex(link.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
            // 20170420 raztoki, ajax
            final String dlbutton = br.getRegex("('|\")(/downloadbutton\\.php\\?name=.*?)\\1").getMatch(1);
            final Browser ajax = br.cloneBrowser();
            // 20170510 dlbutton no longer used... but keep it here anyway.
            if (dlbutton != null) {
                ajax.getHeaders().put("Accept", "*/*");
                ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                getPage(ajax, dlbutton);
            }
            final Form download = ajax.getFormbyActionRegex("https?://(\\w+\\.)file-upload\\.net/download(?:\\d+)?\\.php\\?.*?");
            if (download == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String sitekey = download.getRegex("data\\-sitekey=\"([^<>\"]+)\"").getMatch(0);
            if (download.containsHTML("g\\-recaptcha") && sitekey != null) {
                logger.info("ReCaptchaV2 required");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, ajax, sitekey) {
                    /* 2021-09-13 */
                    @Override
                    public TYPE getType() {
                        return TYPE.INVISIBLE;
                    }
                }.getToken();
                download.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, download, false, 1);
        } else if (new Regex(link.getDownloadURL(), PAT_VIEW).matches()) {
            /* DownloadFiles */
            String downloadurl = br.getRegex("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getMatch(0);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadurl);
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }
}