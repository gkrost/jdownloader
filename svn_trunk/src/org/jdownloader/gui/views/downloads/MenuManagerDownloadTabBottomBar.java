package org.jdownloader.gui.views.downloads;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeparatorData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.ChunksEditorLink;
import org.jdownloader.gui.mainmenu.ParalellDownloadsEditorLink;
import org.jdownloader.gui.mainmenu.ParallelDownloadsPerHostEditorLink;
import org.jdownloader.gui.mainmenu.SpeedlimitEditorLink;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.action.CleanupDownloadFolderAction;
import org.jdownloader.gui.views.downloads.action.GenericDeleteFromDownloadlistAction;
import org.jdownloader.gui.views.downloads.action.ResetAction;
import org.jdownloader.gui.views.downloads.action.ResumeAction;
import org.jdownloader.gui.views.downloads.bottombar.AbstractBottomBarMenuManager;
import org.jdownloader.gui.views.downloads.bottombar.AddLinksContainer;
import org.jdownloader.gui.views.downloads.bottombar.BottomBarMenuManagerAction;
import org.jdownloader.gui.views.downloads.bottombar.DeleteContainer;
import org.jdownloader.gui.views.downloads.bottombar.DownloadsOverviewPanelToggleAction;
import org.jdownloader.gui.views.downloads.bottombar.HorizontalBoxItem;
import org.jdownloader.gui.views.downloads.bottombar.QuickFilterMenuItem;
import org.jdownloader.gui.views.downloads.bottombar.QuickSettingsMenuContainer;
import org.jdownloader.gui.views.downloads.bottombar.SearchMenuItem;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.AutoConfirmStopAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.PasteLinksAction;

public class MenuManagerDownloadTabBottomBar extends AbstractBottomBarMenuManager {
    private static final MenuManagerDownloadTabBottomBar INSTANCE = new MenuManagerDownloadTabBottomBar();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     *
     * @return
     */
    public static MenuManagerDownloadTabBottomBar getInstance() {
        return INSTANCE;
    }

    @Override
    public String getFileExtension() {
        return ".jdDLBottomBar";
    }

    @Override
    protected String getStorageKey() {
        return "DownloadTabBottomBar";
    }

    @Override
    public String getName() {
        return _GUI.T.BottomBarMenuManager_getName();
    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        fillAddLinks(mr);
        mr.add(setName(setIconKey(new ActionData(GenericDeleteFromDownloadlistAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_ALL, true), IconKey.ICON_DELETE), MenuItemData.EMPTY));
        mr.add(CleanupDownloadFolderAction.class);
        DeleteContainer delete = new DeleteContainer();
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_DISABLED, true), IconKey.ICON_REMOVE_DISABLED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_FAILED, true), IconKey.ICON_REMOVE_FAILED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_FINISHED, true), IconKey.ICON_REMOVE_OK));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_OFFLINE, true), IconKey.ICON_REMOVE_OFFLINE));
        mr.add(delete);
        //
        mr.add(new SearchMenuItem());
        mr.add(new HorizontalBoxItem());
        mr.add(new QuickFilterMenuItem());
        QuickSettingsMenuContainer quicksettings = new QuickSettingsMenuContainer();
        quicksettings.add(new ChunksEditorLink());
        quicksettings.add(new ParalellDownloadsEditorLink());
        quicksettings.add(new ParallelDownloadsPerHostEditorLink());
        //
        quicksettings.add(new SpeedlimitEditorLink());
        quicksettings.add(new SeparatorData());
        quicksettings.add(DownloadsPropertiesToggleAction.class);
        quicksettings.add(DownloadsOverviewPanelToggleAction.class);
        quicksettings.add(new SeparatorData());
        quicksettings.add(BottomBarMenuManagerAction.class);
        mr.add(quicksettings);
        final OptionalContainer opt;
        mr.add(opt = new OptionalContainer(false));
        opt.add(new MenuItemData(new ActionData(ResumeAction.class)));
        opt.add(new MenuItemData(new ActionData(ResetAction.class)));
        opt.add(AutoConfirmStopAction.class);
        return mr;
    }

    public static void fillAddLinks(MenuContainerRoot mr) {
        mr.add(AddLinksAction.class);
        AddLinksContainer addLinks = new AddLinksContainer();
        addLinks.add(setName(new ActionData(AddLinksAction.class), _GUI.T.AddOptionsAction_actionPerformed_addlinks()));
        addLinks.add(AddContainerAction.class);
        addLinks.add((setAccelerator(new MenuItemData(new ActionData(PasteLinksAction.class).putSetup(PasteLinksAction.DEEP_DECRYPT_ENABLED, false)), KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()))));
        addLinks.add((setAccelerator(new MenuItemData(new ActionData(PasteLinksAction.class).putSetup(PasteLinksAction.DEEP_DECRYPT_ENABLED, true)), KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK))));
        mr.add(addLinks);
        //
    }
}
