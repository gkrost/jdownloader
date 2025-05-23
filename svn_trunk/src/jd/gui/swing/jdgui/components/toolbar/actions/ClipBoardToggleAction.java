package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.TransferHandler;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.jdgui.Flashable;
import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtJToggleButton;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class ClipBoardToggleAction extends AbstractToolbarToggleAction implements Flashable {
    private final Icon    iconNormal;
    private final Icon    iconHighlight;
    private final Icon    iconSelected;
    private JToggleButton bt;

    public ClipBoardToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED);
        setIconKey(IconKey.ICON_CLIPBOARD);
        iconNormal = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24);
        iconHighlight = NewTheme.I().getCheckBoxImage(getIconKey(), false, 24, new Color(0xFF9393));
        iconSelected = NewTheme.I().getCheckBoxImage(this.getIconKey(), true, 24);
        CFG_GUI.CLIPBOARD_DISABLED_WARNING_FLASH_ENABLED.getEventSender().addListener(this, true);
        if (!getKeyHandler().isEnabled() && CFG_GUI.CLIPBOARD_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
            JDGui.getInstance().getFlashController().register(ClipBoardToggleAction.this);
        } else {
            JDGui.getInstance().getFlashController().unregister(ClipBoardToggleAction.this);
        }
    }

    @Override
    public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, Boolean newValue) {
        super.onConfigValueModified(keyHandler, newValue);
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final boolean isClipboardEnabled = getKeyHandler().isEnabled();
                if (!isClipboardEnabled && CFG_GUI.CLIPBOARD_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
                    JDGui.getInstance().getFlashController().register(ClipBoardToggleAction.this);
                } else {
                    JDGui.getInstance().getFlashController().unregister(ClipBoardToggleAction.this);
                }
            }
        };
    }

    @Override
    public AbstractButton createButton() {
        if (bt != null) {
            return bt;
        }
        bt = new ExtJToggleButton(this);
        bt.setIcon(iconNormal);
        bt.setRolloverIcon(iconNormal);
        bt.setSelectedIcon(iconSelected);
        bt.setRolloverSelectedIcon(iconSelected);
        bt.setHideActionText(true);
        bt.setTransferHandler(new TransferHandler() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport support) {
                return ClipboardMonitoring.hasSupportedTransferData(support.getTransferable());
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                return true;
            }

            @Override
            public boolean importData(TransferSupport support) {
                return importTransferable(support.getTransferable());
            }

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                return importTransferable(t);
            }

            private final boolean importTransferable(final Transferable t) {
                try {
                    ClipboardMonitoring.processSupportedTransferData(t, LinkOrigin.DRAG_DROP_ACTION);
                    return true;
                } catch (Exception e) {
                    LogController.CL().log(e);
                }
                return false;
            }
        });
        return bt;
    }

    @Override
    public void onFlashRegister(long c) {
        onFlash(c);
    }

    @Override
    public void onFlashUnRegister(long c) {
        if (bt != null) {
            bt.setIcon(iconNormal);
            bt.setRolloverIcon(iconNormal);
        }
    }

    @Override
    public boolean onFlash(long l) {
        if (bt == null || !bt.isVisible() || !bt.isDisplayable() || !CFG_GUI.CLIPBOARD_DISABLED_WARNING_FLASH_ENABLED.isEnabled()) {
            return false;
        }
        if (l % 2 != 0) {
            bt.setIcon(iconHighlight);
            bt.setRolloverIcon(iconHighlight);
        } else {
            bt.setIcon(iconNormal);
            bt.setRolloverIcon(iconNormal);
        }
        return true;
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.action_clipboard_observer_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI.T.ClipBoardToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI.T.ClipBoardToggleAction_getNameWhenEnabled_();
    }
}
