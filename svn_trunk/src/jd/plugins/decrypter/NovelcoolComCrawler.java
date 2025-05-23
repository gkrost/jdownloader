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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.NovelcoolCom;

@DecrypterPlugin(revision = "$Revision: 50328 $", interfaceVersion = 3, names = {}, urls = {})
public class NovelcoolComCrawler extends PluginForDecrypt {
    public NovelcoolComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "novelcool.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    private static final String PATTERN_RELATIVE_CHAPTER = "(?i)/chapter/([A-Za-z0-9\\-]+)/(\\d+)/?";
    private static final String PATTERN_RELATIVE_NOVEL   = "(?i)/novel/([\\w\\-]+)\\.html";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_CHAPTER + "|" + PATTERN_RELATIVE_NOVEL + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex regex_chapter = new Regex(param.getCryptedUrl(), PATTERN_RELATIVE_CHAPTER);
        if (regex_chapter.patternFind()) {
            final String chapterSlug = regex_chapter.getMatch(0);
            final String chapterTitleFromSlug = chapterSlug.replace("-", " ").trim();
            /* Find all pictures of a chapter of a novel */
            final String chapterNumber = getChapterNumberFromURL(param.getCryptedUrl());
            final String bookID = br.getRegex("cur_book_id = \"(\\d+)").getMatch(0);
            final String chapterID = br.getRegex("cur_chapter_id = \"(\\d+)").getMatch(0);
            final String seriesTitle = NovelcoolCom.findSeriesTitle(br);
            final String[] urls = br.getRegex("<option[^>]*value=\"(https[^\"]+)\"[^>]*>\\d+/\\d+</option>").getColumn(0);
            if (urls == null || urls.length == 0) {
                if (br.containsHTML("chapter-start-mark")) {
                    /* Download chapter as html page */
                    final DownloadLink chapterAsHTML = this.createDownloadlink(br.getURL() + ".jdeatme");
                    chapterAsHTML.setFinalFileName(chapterTitleFromSlug + ".html");
                    chapterAsHTML.setDownloadSize(br.getRequest().getHtmlCode().getBytes("UTF-8").length);
                    chapterAsHTML.setAvailable(true);
                    ret.add(chapterAsHTML);
                    return ret;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final HashSet<String> dupes = new HashSet<String>();
            int position = 1;
            for (final String url : urls) {
                if (!dupes.add(url)) {
                    continue;
                }
                final DownloadLink image = createDownloadlink(url);
                image.setProperty(NovelcoolCom.PROPERTY_BOOK_ID, bookID);
                image.setProperty(NovelcoolCom.PROPERTY_CHAPTER_ID, chapterID);
                if (seriesTitle != null) {
                    image.setProperty(NovelcoolCom.PROPERTY_SERIES_TITLE, seriesTitle);
                }
                if (chapterNumber != null) {
                    image.setProperty(NovelcoolCom.PROPERTY_CHAPTER_NUMBER, chapterNumber);
                }
                image.setProperty(NovelcoolCom.PROPERTY_PAGE_NUMBER, position);
                image.setProperty(NovelcoolCom.PROPERTY_PAGE_MAX, urls.length);
                image.setName(NovelcoolCom.formatFilename(image));
                image.setAvailable(true);
                ret.add(image);
                position++;
            }
            final String chapterNumberForPackagename;
            if (chapterNumber != null) {
                chapterNumberForPackagename = chapterNumber;
            } else {
                chapterNumberForPackagename = chapterID;
            }
            final FilePackage fp = FilePackage.getInstance();
            if (seriesTitle != null) {
                fp.setName(seriesTitle + " - " + chapterNumberForPackagename);
            } else {
                fp.setName("Unknown novel" + " - " + chapterNumberForPackagename);
            }
            fp.addLinks(ret);
        } else {
            /* Find all chapters of a novel */
            final String coverurl = br.getRegex("BOOK_COVER\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
            final String seriesTitle = br.getRegex("BOOK_NAME\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            if (coverurl != null) {
                final DownloadLink cover = this.createDownloadlink(coverurl);
                if (seriesTitle != null) {
                    cover.setName(seriesTitle + ".jpg");
                }
                cover.setAvailable(true);
                ret.add(cover);
            } else {
                logger.warning("Failed to find coverurl");
            }
            final String[] links = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            if (links != null && links.length > 0) {
                for (final String link : links) {
                    if (link.matches(".+" + PATTERN_RELATIVE_CHAPTER)) {
                        ret.add(this.createDownloadlink(br.getURL(link).toExternalForm()));
                    }
                }
            } else {
                logger.warning("Failed to find any chapters");
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    public static String getChapterNumberFromURL(final String url) {
        String chapterNumber = new Regex(url, "(?i)Chapter-([0-9\\-]+)").getMatch(0);
        if (chapterNumber == null) {
            chapterNumber = new Regex(url, "(?i)(?:Cap-tulo|Capitulo)-([0-9\\-]+)").getMatch(0);
        }
        if (chapterNumber != null) {
            chapterNumber = chapterNumber.replace("-", ".");
        }
        return chapterNumber;
    }
}
