package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 50866 $", interfaceVersion = 2, names = { "livestream.com" }, urls = { "https?://(www\\.)?livestream\\.com/[^<>\"]+/events/\\d+/?$" })
public class LiveStreamCom extends PluginForDecrypt {
    public LiveStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String title = br.getRegex("<title>\\s*(.*?)\\s*(?:on\\s*Livestream)?\\s*</title>").getMatch(0);
        final String eventsID = new Regex(param.getCryptedUrl(), "/events/(\\d+)").getMatch(0);
        final String accountID = br.getRegex("/accounts/(\\d+)/events/" + eventsID).getMatch(0);
        if (accountID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp;
        if (title != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlOnlyDecode(title));
        } else {
            fp = null;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        Long lastEntryID = null;
        final int stepSize = 50;
        int done = 0;
        while (!isAbort()) {
            if (lastEntryID == null) {
                br.getPage("https://api.new.livestream.com/accounts/" + accountID + "/events/" + eventsID + "/feed.json?type=video&newer=-1&older=" + stepSize);
            } else {
                br.getPage("https://api.new.livestream.com/accounts/" + accountID + "/events/" + eventsID + "/feed.json?id=" + lastEntryID + "&type=video&newer=-1&older=" + stepSize);
            }
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final List<Map<String, Object>> data = (List<Map<String, Object>>) entries.get("data");
            if (data == null || data.size() == 0) {
                break;
            }
            for (Map<String, Object> entry : data) {
                done++;
                final Map<String, Object> entryData = (Map<String, Object>) entry.get("data");
                final long entryID = JavaScriptEngineFactory.toLong(entryData.get("id"), -1);
                final String type = (String) entry.get("type");
                if (type == null || !type.equals("video")) {
                    logger.info("Unsupported type:" + type + "|id:" + entryID);
                    continue;
                }
                if (entryID != -1) {
                    lastEntryID = entryID;
                }
                final String caption = (String) entryData.get("caption");
                if (entryID != -1 && caption != null) {
                    final DownloadLink link = createDownloadlink(param + "//videos/" + entryID);
                    link.setName(caption + ".mp4");
                    link.setAvailable(true);
                    ret.add(link);
                    fp.add(link);
                }
            }
            final long total = JavaScriptEngineFactory.toLong(entries.get("total"), -1);
            if (total != -1 && done >= total) {
                break;
            }
        }
        return ret;
    }
}
