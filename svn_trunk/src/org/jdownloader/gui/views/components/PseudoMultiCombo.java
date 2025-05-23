package org.jdownloader.gui.views.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.appwork.sunwrapper.sun.swing.SwingUtilities2Wrapper;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.JScrollPopupMenu;
import org.appwork.utils.CompareUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class PseudoMultiCombo<Type> extends ExtButton {
    protected HashSet<Type> selectedItems = new HashSet<Type>();
    private List<Type>      values        = new ArrayList<Type>();
    private boolean         popDown       = true;

    public PseudoMultiCombo(List<Type> values) {
        super();
        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onPopup();
            }
        });
        this.setHorizontalAlignment(SwingConstants.LEFT);
        Insets m = getMargin();
        if (m == null) {
            m = new Insets(0, 0, 0, 0);
        }
        m.right = getPopIcon(true).getIconWidth() + 5;
        m.left = 1;
        setMargin(m);
        setValues(values);
    }

    public PseudoMultiCombo(Type[] values) {
        this(Arrays.asList(values));
    }

    public void setValues(List<Type> values) {
        this.values.clear();
        for (Type v : values) {
            this.values.add(v);
        }
        updateLabel();
    }

    public void setValues(Type[] values) {
        setValues(Arrays.asList(values));
    }

    public List<Type> getValues() {
        return Collections.unmodifiableList(values);
    }

    protected Icon getPopIcon(boolean closed) {
        if (closed) {
            if (isPopDown()) {
                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
            } else {
                return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
            }
        } else {
            if (isPopDown()) {
                return NewTheme.I().getIcon(IconKey.ICON_POPUPLARGE, -1);
            } else {
                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNLARGE, -1);
            }
        }
    }

    public boolean isPopDown() {
        return popDown;
    }

    public void setPopDown(boolean popDown) {
        this.popDown = popDown;
    }

    protected Icon getIcon(Type[] value) {
        return null;
    }

    private long    lastHide = 0;
    private boolean closed   = true;
    private String  orgText;

    public ExtRealCheckBoxMenuItem createMenuItem(final Type sc, BasicAction appAction) {
        return new ExtRealCheckBoxMenuItem(appAction) {
            @Override
            protected void updateIcon() {
                Icon icon = PseudoMultiCombo.this.getIcon(sc);
                if (icon == null) {
                    super.updateIcon();
                } else {
                    if (isSelected()) {
                        setIcon(new MergedIcon(selIcon, icon));
                    } else {
                        setIcon(new MergedIcon(unselIcon, icon));
                    }
                }
            }
        };
    }

    protected void onPopup() {
        long timeSinceLastHide = System.currentTimeMillis() - lastHide;
        if (timeSinceLastHide < 250) {
            //
            return;
        }
        JScrollPopupMenu popup = new JScrollPopupMenu() {
            @Override
            public void setVisible(boolean b) {
                if (!b) {
                    lastHide = System.currentTimeMillis();
                }
                super.setVisible(b);
                closed = true;
                PseudoMultiCombo.this.repaint();
            }
        };
        final AtomicInteger integer = new AtomicInteger(0);
        for (final Type sc : values) {
            ExtRealCheckBoxMenuItem mi;
            popup.add(mi = createMenuItem(sc, new AppAction() {
                private Type value;
                {
                    value = sc;
                    setName(getLabel(integer.get(), sc));
                    setSmallIcon(PseudoMultiCombo.this.getIcon(integer.get(), sc));
                    setSelected(isItemSelected(sc));
                    integer.incrementAndGet();
                }

                public void setSelected(final boolean selected) {
                    super.setSelected(selected);
                }

                public void actionPerformed(ActionEvent e) {
                    setSelected(isSelected());
                    System.out.println(isSelected());
                    setItemSelected(value, isSelected());
                }
            }));
            mi.setHideOnClick(false);
        }
        Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();
        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(new Dimension((int) Math.max(getWidth() + insets.left + insets.right, pref.getWidth()), (int) pref.getHeight()));
        // PseudoCombo.this.repaint();
        if (isPopDown()) {
            popup.show(this, -insets.left, getHeight() + insets.top);
        } else {
            popup.show(this, -insets.left, -popup.getPreferredSize().height + insets.bottom);
        }
        closed = false;
    }

    public boolean isItemSelected(Type sc) {
        return selectedItems.contains(sc);
    }

    public void setItemSelected(Type value, boolean b) {
        if (b) {
            if (selectedItems.add(value)) {
                updateLabel();
                onChanged();
            }
        } else {
            if (selectedItems.remove(value)) {
                updateLabel();
                onChanged();
            }
        }
    }

    protected Icon getIcon(int i, Type sc) {
        return getIcon(sc);
    }

    protected Icon getIcon(Type sc) {
        return null;
    }

    protected String getLabel(int i, Type sc) {
        return getLabel(sc);
    }

    protected String getLabel(Type sc) {
        if (sc instanceof LabelInterface) {
            return ((LabelInterface) sc).getLabel();
        }
        return sc + "";
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Icon icon = getPopIcon(closed);
        if (!isEnabled()) {
            icon = org.jdownloader.images.NewTheme.I().getDisabledIcon(icon);
        }
        icon.paintIcon(this, g, getWidth() - icon.getIconWidth() - 5, (getHeight() - icon.getIconHeight()) / 2);
    }

    public void setSelectedItems(Type... value) {
        selectedItems.clear();
        if (value != null) {
            for (Type t : value) {
                selectedItems.add(t);
            }
        }
        onChanged();
        updateLabel();
    }

    public void setSelectedItems(List<Type> value) {
        selectedItems.clear();
        if (value != null) {
            for (Type t : value) {
                selectedItems.add(t);
            }
        }
        onChanged();
        updateLabel();
    }

    public void setText(String text) {
        String oldValue = this.orgText;
        this.orgText = text;
        firePropertyChange(TEXT_CHANGED_PROPERTY, oldValue, text);
        if (text == null || oldValue == null || !text.equals(oldValue)) {
            revalidate();
            repaint();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension ret = super.getMinimumSize();
        ret.width = ret.height;
        return ret;
    }

    @Override
    public String getText() {
        if (orgText == null) {
            return null;
        }
        return SwingUtilities2Wrapper.clipStringIfNecessary(this, this.getFontMetrics(this.getFont()), orgText, getWidth() - getMargin().left - getMargin().right - 10);
    }

    private void updateLabel() {
        List<Type> list = new ArrayList<Type>(selectedItems);
        Collections.sort(list, getComparator());
        setText(getLabel(list));
        setIcon(getIcon(list));
        setToolTipText(getToolTip(list));
    }

    protected String getToolTip(List<Type> list) {
        if (list.size() == 0) {
            return _GUI.T.PseudoMultiCombo_nothing();
        }
        StringBuilder sb = new StringBuilder();
        for (Type t : list) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            if (t instanceof TooltipInterface) {
                sb.append(((TooltipInterface) t).getTooltip());
            } else {
                sb.append(getLabel(t));
            }
        }
        return sb.toString();
    }

    protected Icon getIcon(List<Type> list) {
        return null;
    }

    protected String getLabel(List<Type> list) {
        if (list.size() == 0) {
            return _GUI.T.PseudoMultiCombo_nothing();
        }
        StringBuilder sb = new StringBuilder();
        for (Type t : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(getLabel(t));
        }
        return sb.toString();
    }

    private Comparator<? super Type> getComparator() {
        return new Comparator<Type>() {
            @Override
            public int compare(Type o1, Type o2) {
                // sort based in the order of values. this is probably not the fastest solution
                return CompareUtils.compareInt(values.indexOf(o1), values.indexOf(o2));
            }
        };
    }

    public void onChanged() {
    }

    public List<Type> getSelectedItems() {
        ArrayList<Type> ret = new ArrayList<Type>(selectedItems);
        Collections.sort(ret, getComparator());
        return ret;
    }
}
