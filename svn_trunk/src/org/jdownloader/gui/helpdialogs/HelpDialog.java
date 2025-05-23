package org.jdownloader.gui.helpdialogs;

import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.locator.DialogLocator;
import org.appwork.utils.swing.locator.AbstractLocator;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.RestartController;

public class HelpDialog {
    public static void showIfAllowed(final MessageConfig config) {
        if (!CFG_GUI.CFG.isHelpDialogsEnabled()) {
            /* Do not show dialog */
            return;
        }
        show(config.getPoint(), config.getDontShowAgainKey(), config.getFlags(), config.getTitle(), config.getMsg(), config.getIcon());
    }

    public static void show(final Point point, final String dontShowAgainKey, int flags, String title, String msg, Icon icon) {
        show(null, null, point, dontShowAgainKey, flags, title, msg, icon);
    }

    public static void showCaptchaSkippedDialog() {
        if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
            HelpDialog.show(false, true, ToolTipController.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), new AbstractIcon(IconKey.ICON_SKIPPED, 32));
        }
    }

    public static void show(final Boolean expandToBottom, final Boolean expandToRight, final Point point, final String dontShowAgainKey, int flags, String title, String msg, Icon icon) {
        final boolean test = RestartController.getInstance().getParameterParser(null).hasCommandSwitch("translatortest");
        if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isBalloonNotificationEnabled()) {
            return;
        }
        if (dontShowAgainKey != null) {
            final Integer ret = JSonStorage.getPlainStorage("Dialogs").get(dontShowAgainKey, -1);
            if (ret != null && ret > 0) {
                return;
            }
        }
        try {
            final ConfirmDialog d = new ConfirmDialog(flags | UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, title, _GUI.T.literall_usage_tipp() + "\r\n\r\n..." + msg, icon, null, null) {
                {
                    if (point != null) {
                        setLocator(new DialogLocator() {
                            @Override
                            public Point getLocationOnScreen(AbstractDialog<?> abstractDialog) {
                                if (Boolean.FALSE.equals(expandToBottom)) {
                                    point.y -= abstractDialog.getPreferredSize().height;
                                }
                                if (Boolean.FALSE.equals(expandToRight)) {
                                    point.x -= abstractDialog.getPreferredSize().width;
                                }
                                return AbstractLocator.correct(point, abstractDialog.getDialog());
                            }

                            @Override
                            public void onClose(AbstractDialog<?> abstractDialog, final ComponentEvent event) {
                            }
                        });
                    }
                }

                @Override
                public String getDontShowAgainKey() {
                    if (test) {
                        return "bla_" + System.currentTimeMillis();
                    }
                    if (dontShowAgainKey == null) {
                        return super.getDontShowAgainKey();
                    }
                    return dontShowAgainKey;
                }

                public void windowClosing(final WindowEvent arg0) {
                    setReturnmask(false);
                    this.dispose();
                }
            };
            if (BinaryLogic.containsAll(flags, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN)) {
                d.setDoNotShowAgainSelected(true);
            }
            Integer ret = JSonStorage.getPlainStorage("Dialogs").get(d.getDontShowAgainKey(), -1);
            if (ret != null && ret > 0) {
                return;
            }
            d.show();
        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
    }

    public static void show(int flags, String title, String msg, Icon icon) {
        show(null, title, flags, title, msg, icon);
    }
}
