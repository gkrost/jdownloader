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
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.images.svg;

import java.awt.Color;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.appwork.utils.images.ScalableIcon;

/**
 * @author thomas
 * @date 16.01.2023
 *
 */
public class KitFoxFactory implements SVGFactory {
    /**
     *
     */
    public KitFoxFactory() {
    }

    protected SVGIO newInstance() {
        return new SVGIO(this);
    }

    @Override
    public Image getImageFromSVG(URI resource, int width, int height) throws IOException {
        return newInstance().getImageFromSVG(resource, width, height);
    }

    @Override
    public Image getImageFromSVG(URI resource, int width, int height, Color color) throws IOException {
        return newInstance().getImageFromSVG(resource, width, height, color);
    }

    @Override
    public Image getImageFromSVG(InputStream inputStream, URI base, int width, int height, Color color) throws IOException {
        return newInstance().getImageFromSVG(inputStream, base, width, height, color);
    }

    @Override
    public boolean isSupported() {
        // https://github.com/blackears/svgSalamander/tree/ecd1092927971a9e02a9062fdd1a95dd610d7309
        // self compiled for Java 1.6 compatibility
        return true;// JVMVersion.isMinimum(JVMVersion.JAVA_1_8);
    }

    /**
     * @see org.appwork.utils.images.svg.SVGFactory#getIconFromSVG(java.io.InputStream, URI, int, int, java.awt.Color)
     */
    @Override
    public ScalableIcon getIconFromSVG(InputStream stream, URI base, int width, int height, Color color) throws IOException {
        return newInstance().getIconFromSVG(stream, base, width, height, color);
    }

    /**
     * @see org.appwork.utils.images.svg.SVGFactory#openInputStream(java.net.URI)
     */
    @Override
    public InputStream openInputStream(URI uri) throws IOException {
        return uri.toURL().openStream();
    }
}
