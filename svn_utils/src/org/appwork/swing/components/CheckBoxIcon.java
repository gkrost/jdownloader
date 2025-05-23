/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2025, AppWork GmbH <e-mail@appwork.org>
 *         Spalter Strasse 58
 *         91183 Abenberg
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.swing.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JCheckBox;

import org.appwork.loggingv3.LogV3;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.ScalableIcon;

public final class CheckBoxIcon implements Icon, ScalableIcon, IDIcon {
    public static final Icon FALSE     = (new CheckBoxIcon(false));
    public static final Icon TRUE      = (new CheckBoxIcon(true));
    public static final Icon UNDEFINED = (new CheckBoxIcon(true, false));
    private int              size;
    private final JCheckBox  checkBox;
    private Rectangle2D      unscaledDimensionAndPosition;
    private final Image      image;
    private boolean          selected;
    private boolean          enabled;

    public CheckBoxIcon(final boolean selected, final boolean enabled) {
        this(-1, selected, enabled);
    }

    protected Image buildHeadlessCheckBoxIcon(int size, boolean selected, boolean enabled) {
        final Image ret;
        if (selected) {
            ret = HeadlessCheckboxIconRef.HEADLESS_checkbox_true.image(size);
        } else {
            ret = HeadlessCheckboxIconRef.HEADLESS_checkbox_false.image(size);
        }
        if (!enabled) {
            return IconIO.toGrayScale(ret);
        }
        return ret;
    }

    public CheckBoxIcon(int size, boolean selected, boolean enabled) {
        this.selected = selected;
        this.enabled = enabled;
        this.size = size;
        if (Application.isHeadless()) {
            checkBox = null;
            image = buildHeadlessCheckBoxIcon(size, selected, enabled);
        } else {
            Image image = null;
            JCheckBox checkBox = null;
            try {
                checkBox = new JCheckBox();
                checkBox.setEnabled(enabled);
                checkBox.setSelected(selected);
                // checkBox.setDebugGraphicsOptions(DebugGraphics.LOG_OPTION);
                checkBox.setSize(checkBox.getPreferredSize());
                checkBox.setOpaque(false);
                checkBox.setContentAreaFilled(true);
                BufferedImage dummy = IconIO.createEmptyImage(32, 32);
                Graphics2D g2d = (Graphics2D) dummy.getGraphics();
                g2d.setTransform(new AffineTransform());
                RecordingGraphics2D record = new RecordingGraphics2D(g2d);
                checkBox.paint(record);
                g2d.dispose();
                unscaledDimensionAndPosition = record.getCompleteDrawnArea();
            } catch (Exception e) {
                checkBox = null;
                LogV3.log(e);
                image = buildHeadlessCheckBoxIcon(size, selected, enabled);
            }
            this.checkBox = checkBox;
            this.image = image;
        }
    }

    /**
     * @see javax.swing.ImageIcon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    @Override
    public synchronized void paintIcon(Component c, Graphics gOrg, int x, int y) {
        paintIcon(c, gOrg, x, y, getIconWidth(), getIconHeight());
    }

    public CheckBoxIcon(boolean selected) {
        this(selected, true);
    }

    /**
     * @see javax.swing.Icon#getIconWidth()
     */
    @Override
    public int getIconWidth() {
        if (image != null) {
            return image.getWidth(null);
        }
        if (size <= 0) {
            return (int) unscaledDimensionAndPosition.getWidth();
        }
        return size;
    }

    /**
     * @see javax.swing.Icon#getIconHeight()
     */
    @Override
    public int getIconHeight() {
        if (image != null) {
            return image.getHeight(null);
        }
        if (size <= 0) {
            return (int) unscaledDimensionAndPosition.getHeight();
        }
        return size;
    }

    /**
     * @see org.appwork.swing.components.IDIcon#getIdentifier()
     */
    @Override
    public IconIdentifier getIdentifier() {
        return new IconIdentifier("CheckBoxIcon_" + (selected ? "true" : "false") + (enabled ? "_enabled" : "_disabled"));
    }

    /**
     * @see org.appwork.utils.images.ScalableIcon#setIconHeight(int)
     */
    @Override
    public void setIconHeight(int iconHeight) {
        size = iconHeight;
    }

    /**
     * @see org.appwork.utils.images.ScalableIcon#setIconWidth(int)
     */
    @Override
    public void setIconWidth(int iconWidth) {
        size = iconWidth;
    }

    /**
     * @see org.appwork.utils.images.ScalableIcon#paintIcon(java.awt.Component, java.awt.Graphics, int, int, int, int)
     */
    @Override
    public void paintIcon(Component c, Graphics gOrg, int x, int y, int width, int height) {
        if (image != null) {
            // headless
            gOrg.drawImage(IconIO.getScaledInstance(image, width, height), x, y, c);
            return;
        }
        Graphics g = gOrg.create();
        // checkBox.setBackground(new Color(0, 0, 0, 0f));
        Graphics2D g2d = ((Graphics2D) g);
        // AffineTransform orgTf2 = g2d.getTransform();
        // g2d.setColor(Color.green);
        g2d.translate(x, y);
        AffineTransform orgTf = g2d.getTransform();
        // g2d.fillRect(x + 1, y + 1, getIconWidth() - 2, getIconHeight() - 2);
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // dummy paint just to get the painted area
        // g2d.setTransform(new AffineTransform());
        double componentScaleX = height / unscaledDimensionAndPosition.getWidth();
        double componentScaleY = width / unscaledDimensionAndPosition.getHeight();
        // g2d.scale(orgTf.getScaleX(), orgTf.getScaleY());
        // g2d.translate(x + area.getX() - 1, y + area.getY() + 1);
        // g2d.translate(x, y);
        // g2d.translate((orgTf.getTranslateX()), (orgTf.getTranslateY()));
        g2d.scale(componentScaleX, componentScaleY);
        // RecordingGraphics2D record2 = new RecordingGraphics2D(g2d);
        // checkBox.paint(record2);
        // Rectangle2D area2 = record2.getCompleteDrawnArea();
        g2d.translate(-unscaledDimensionAndPosition.getX(), -unscaledDimensionAndPosition.getY());
        // g2d.translate(-area2.getX() / record2.getTransform().getScaleX() * orgTf.getScaleX(), -area2.getY() /
        // record2.getTransform().getScaleY() * orgTf.getScaleY());
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && false) {
            System.out.println(g2d.getTransform());
            // validation check, if the component actually paints in the desired size
            RecordingGraphics2D record3 = new RecordingGraphics2D(g2d);
            checkBox.paint(record3);
            Rectangle2D area3 = record3.getCompleteDrawnArea();
            double sizeX = (int) Math.round(area3.getWidth() / orgTf.getScaleX());
            double sizeY = (int) Math.round(area3.getHeight() / orgTf.getScaleY());
            DebugMode.breakIf((int) sizeX != getIconWidth());
            DebugMode.breakIf((int) sizeY != getIconHeight());
        }
        // System.out.println(g2d.getTransform());
        // System.out.println(g2d.getTransform());
        // g2d.setClip(area3);
        checkBox.paint(g2d);
        g2d.dispose();
        // BufferedImage crop1 = image.getSubimage((int) (area.getX()), (int) (area.getY()), (int) (area.getWidth() + 1), (int)
        // (area.getHeight() + 1));
        // if (true) {
        // //
        // super.paintIcon(c, g, x, y);
        // return;
        // } else {
        // // might become not sharp
        // Object restore = ((Graphics2D) g).getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        // try {
        // ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, Interpolation.BICUBIC.getHint());
        // super.paintIcon(c, g, x, y);
        // } finally {
        // if (restore == null) {
        // restore = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        // }
        // ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, restore);
        // }
        // }
    }
}
