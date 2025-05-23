package org.jdownloader.plugins.components.archiveorg;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Time;

import jd.http.Cookies;

public class ArchiveOrgLendingInfo {
    private Cookies              cookies         = null;
    private Long                 timestampLoaned = null;
    private final long           timestampCreated;
    private final List<BookPage> bookPages       = new ArrayList<BookPage>();

    public ArchiveOrgLendingInfo(final Cookies cookies) {
        this.cookies = cookies;
        if (cookies != null) {
            setCookies(cookies);
        }
        timestampCreated = Time.systemIndependentCurrentJVMTimeMillis();
    }

    private void updateLoanTimestamp() {
        this.timestampLoaned = Time.systemIndependentCurrentJVMTimeMillis();
    }

    public Cookies getCookies() {
        return this.cookies;
    }

    public void setCookies(final Cookies cookies) {
        this.cookies = cookies;
        /* Assume that book has been loaned */
        this.updateLoanTimestamp();
    }

    /** Returns whether or not this session is considered valid or worth keeping. */
    public synchronized boolean isValid() {
        if (isLoanSessionActive()) {
            /* Active loan session */
            return true;
        } else if (Time.systemIndependentCurrentJVMTimeMillis() - this.timestampCreated < 10 * 60 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean isLoanSessionActive() {
        if (this.timestampLoaned != null && Time.systemIndependentCurrentJVMTimeMillis() - this.timestampLoaned.longValue() < 60 * 60 * 1000) {
            /* Active loan session */
            return true;
        } else {
            return false;
        }
    }

    /** If this returns null, loan is allowed immediately. */
    public synchronized Long getTimeUntilNextLoanAllowed() {
        if (this.timestampLoaned == null) {
            return null;
        }
        final long timePassedSinceLastLoan = Time.systemIndependentCurrentJVMTimeMillis() - this.timestampLoaned.longValue();
        final long maxLoanTimeframeMillis = 5 * 60 * 1000;
        if (timePassedSinceLastLoan < maxLoanTimeframeMillis) {
            /* Book has been loaned within the last 5 minutes -> Wait at least 5 minutes between loaning. */
            return maxLoanTimeframeMillis - timePassedSinceLastLoan;
        } else {
            return null;
        }
    }

    /** Returns true if this session has been newly added within the last X minutes. */
    public synchronized boolean hasJustBeenLoaned() {
        if (getTimeUntilNextLoanAllowed() == null) {
            return false;
        } else {
            return true;
        }
    }

    public synchronized void setPageURLs(final List<String> urls) {
        this.bookPages.clear();
        for (final String url : urls) {
            this.bookPages.add(new BookPage(url));
        }
    }

    public synchronized void updateOrAddBookPages(final List<String> urls) {
        if (urls.size() == this.bookPages.size()) {
            int index = 0;
            for (final String url : urls) {
                final BookPage existingBookPage = this.bookPages.get(index);
                existingBookPage.setUrl(url);
                index++;
            }
        } else {
            this.setPageURLs(urls);
        }
    }

    /** Returns URL to desired pageIndexNumber. */
    public synchronized String getPageURL(final int pageIndexNumber) {
        final BookPage page = getBookPage(pageIndexNumber);
        if (page != null) {
            return page.getUrl();
        } else {
            return null;
        }
    }

    public synchronized BookPage getBookPage(final int pageIndexNumber) {
        if (pageIndexNumber > -1 && pageIndexNumber < this.bookPages.size()) {
            return this.bookPages.get(pageIndexNumber);
        } else {
            return null;
        }
    }

    public synchronized void setBookPageDownloadStatus(final int pageIndexNumber, final boolean downloaded) {
        final BookPage page = getBookPage(pageIndexNumber);
        if (page != null) {
            page.setDownloaded(downloaded);
        }
    }

    public synchronized boolean looksLikeBookDownloadIsComplete() {
        for (final BookPage page : this.bookPages) {
            if (!page.isDownloaded()) {
                /* At least one page has not been downloaded --> Download of book is not complete. */
                return false;
            }
        }
        /* All pages have been downloaded --> Download of book is complete */
        return true;
    }

    public synchronized int getTotalNumberofPages() {
        if (bookPages == null) {
            return 0;
        } else {
            return bookPages.size();
        }
    }

    public synchronized int getNumberOfDownloadedPages() {
        int num = 0;
        for (final BookPage page : this.bookPages) {
            if (page.isDownloaded()) {
                /* At least one page has not been downloaded --> Download of book is not complete. */
                num++;
            }
        }
        return num;
    }
}

class BookPage {
    private String  url          = null;
    private boolean isDownloaded = false;

    public BookPage(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDownloaded(final boolean downloaded) {
        this.isDownloaded = downloaded;
    }

    public boolean isDownloaded() {
        return this.isDownloaded;
    }
}