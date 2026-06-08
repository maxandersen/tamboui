/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Renders images using the Kitty Graphics Protocol with Unicode placeholders.
 * <p>
 * Instead of re-transmitting the full image payload on every frame, this protocol
 * transmits the image data once (with a virtual placement via {@code U=1}), then
 * renders Unicode placeholder characters ({@code U+10EEEE}) into the buffer cells.
 * The terminal composites the image behind the placeholder text automatically.
 * <p>
 * Because the placeholders are ordinary buffer cells, the diff engine handles them
 * naturally: an unchanged image produces identical cells, so the diff skips them.
 * When the image widget is removed, the placeholder cells are overwritten with spaces
 * by normal rendering, and the terminal removes the image automatically.
 * <p>
 * This approach eliminates <em>all</em> per-frame retransmission and terminal-side
 * memory growth — the image is transmitted exactly once per unique content.
 *
 * <h2>Terminal support</h2>
 * Unicode placeholders require Kitty &ge; 0.28.0 or a terminal that implements this
 * part of the Kitty protocol. Known support:
 * <ul>
 *   <li><b>Kitty</b> &mdash; reference implementation</li>
 *   <li><b>Ghostty</b> &mdash; full support</li>
 * </ul>
 * Terminals that implement the Kitty graphics protocol but <em>not</em> the unicode
 * placeholder extension (WezTerm, Konsole, Warp) should use {@link KittyProtocol}
 * instead, which transmits via raw output with stable image ids and redraw suppression.
 *
 * <h2>Protocol overview</h2>
 * <ol>
 *   <li>Transmit image data with {@code a=T,U=1} (virtual placement, no display).</li>
 *   <li>Fill the display area with {@code U+10EEEE} characters. The image id is encoded
 *       in the foreground color (24-bit RGB = lower 3 bytes of the id). Row and column
 *       positions are encoded via combining diacritics from the Kitty spec.</li>
 *   <li>On subsequent frames, the buffer already contains the same placeholder cells,
 *       so the diff engine emits nothing — zero terminal traffic.</li>
 * </ol>
 *
 * @see <a href="https://sw.kovidgoyal.net/kitty/graphics-protocol/#unicode-placeholders">
 *     Kitty Graphics Protocol — Unicode Placeholders</a>
 * @see KittyProtocol
 */
public final class KittyUnicodePlaceholderProtocol implements ImageProtocol {

    private static final String APC = "\033_G";
    private static final String ST = "\033\\";
    private static final int CHUNK_SIZE = 4096;

    /**
     * The Unicode code point used as the placeholder character.
     * This is a Private Use Area character designated by the Kitty protocol.
     */
    private static final String PLACEHOLDER = new String(Character.toChars(0x10EEEE));

    /**
     * Row/column diacritics from the Kitty protocol specification.
     * Each diacritic encodes an index value (0-based) for the row or column position.
     *
     * @see <a href="https://sw.kovidgoyal.net/kitty/_downloads/f0a0de9ec8d9ff4456206db8e0814937/rowcolumn-diacritics.txt">
     *     rowcolumn-diacritics.txt</a>
     */
    private static final char[] DIACRITICS = {
        '\u0305', '\u030D', '\u030E', '\u0310', '\u0312', '\u033D', '\u033E', '\u033F',
        '\u0346', '\u034A', '\u034B', '\u034C', '\u0350', '\u0351', '\u0352', '\u0357',
        '\u035B', '\u0363', '\u0364', '\u0365', '\u0366', '\u0367', '\u0368', '\u0369',
        '\u036A', '\u036B', '\u036C', '\u036D', '\u036E', '\u036F', '\u0483', '\u0484',
        '\u0485', '\u0486', '\u0487', '\u0592', '\u0593', '\u0594', '\u0595', '\u0597',
        '\u0598', '\u0599', '\u059C', '\u059D', '\u059E', '\u059F', '\u05A0', '\u05A1',
        '\u05A8', '\u05A9', '\u05AB', '\u05AC', '\u05AF', '\u05C4', '\u0610', '\u0611',
        '\u0612', '\u0613', '\u0614', '\u0615', '\u0616', '\u0617', '\u0657', '\u0658',
        '\u0659', '\u065A', '\u065B', '\u065D', '\u065E', '\u06D6', '\u06D7', '\u06D8',
        '\u06D9', '\u06DA', '\u06DB', '\u06DC', '\u06DF', '\u06E0', '\u06E1', '\u06E2',
        '\u06E4', '\u06E7', '\u06E8', '\u06EB', '\u06EC', '\u0730', '\u0732', '\u0733',
        '\u0735', '\u0736', '\u073A', '\u073D', '\u073F', '\u0740', '\u0741', '\u0743',
        '\u0745', '\u0747', '\u0749', '\u074A', '\u07EB', '\u07EC', '\u07ED', '\u07EE',
        '\u07EF', '\u07F0', '\u07F1', '\u07F3', '\u0816', '\u0817', '\u0818', '\u0819',
        '\u081B', '\u081C', '\u081D', '\u081E', '\u081F', '\u0820', '\u0821', '\u0822',
        '\u0823', '\u0825', '\u0826', '\u0827', '\u0829', '\u082A', '\u082B', '\u082C',
        '\u082D', '\u0951', '\u0953', '\u0954', '\u0F82', '\u0F83', '\u0F86', '\u0F87',
        '\u135D', '\u135E', '\u135F', '\u17DD', '\u193A', '\u1A17', '\u1A75', '\u1A76',
        '\u1A77', '\u1A78', '\u1A79', '\u1A7A', '\u1A7B', '\u1A7C', '\u1B6B', '\u1B6D',
        '\u1B6E', '\u1B6F', '\u1B70', '\u1B71', '\u1B72', '\u1B73', '\u1CD0', '\u1CD1',
        '\u1CD2', '\u1CDA', '\u1CDB', '\u1CE0', '\u1DC0', '\u1DC1', '\u1DC3', '\u1DC4',
        '\u1DC5', '\u1DC6', '\u1DC7', '\u1DC8', '\u1DC9', '\u1DCB', '\u1DCC', '\u1DD1',
        '\u1DD2', '\u1DD3', '\u1DD4', '\u1DD5', '\u1DD6', '\u1DD7', '\u1DD8', '\u1DD9',
        '\u1DDA', '\u1DDB', '\u1DDC', '\u1DDD', '\u1DDE', '\u1DDF', '\u1DE0', '\u1DE1',
        '\u1DE2', '\u1DE3', '\u1DE4', '\u1DE5', '\u1DE6', '\u1DFE', '\u20D0', '\u20D1',
        '\u20D4', '\u20D5', '\u20D6', '\u20D7', '\u20DB', '\u20DC', '\u20E1', '\u20E7',
        '\u20E9', '\u20F0', '\u2CEF', '\u2CF0', '\u2CF1', '\u2DE0', '\u2DE1', '\u2DE2',
        '\u2DE3', '\u2DE4', '\u2DE5', '\u2DE6', '\u2DE7', '\u2DE8', '\u2DE9', '\u2DEA',
        '\u2DEB', '\u2DEC', '\u2DED', '\u2DEE', '\u2DEF', '\u2DF0', '\u2DF1', '\u2DF2',
        '\u2DF3', '\u2DF4', '\u2DF5', '\u2DF6', '\u2DF7', '\u2DF8', '\u2DF9', '\u2DFA',
        '\u2DFB', '\u2DFC', '\u2DFD', '\u2DFE', '\u2DFF', '\uA66F', '\uA67C', '\uA67D',
        '\uA6F0', '\uA6F1', '\uA8E0', '\uA8E1', '\uA8E2', '\uA8E3', '\uA8E4', '\uA8E5',
        '\uA8E6', '\uA8E7', '\uA8E8', '\uA8E9', '\uA8EA', '\uA8EB', '\uA8EC', '\uA8ED',
        '\uA8EE', '\uA8EF', '\uA8F0', '\uA8F1', '\uAAB0', '\uAAB2', '\uAAB3', '\uAAB7',
        '\uAAB8', '\uAABE', '\uAABF', '\uAAC1', '\uFE20', '\uFE21', '\uFE22', '\uFE23',
        '\uFE24', '\uFE25', '\uFE26',
    };

    /**
     * Maximum number of rows/columns supported by the diacritics table.
     */
    static final int MAX_DIACRITICS = DIACRITICS.length;

    private final NativeImageCache cache = new NativeImageCache();

    /**
     * Creates a new Kitty unicode placeholder protocol instance.
     */
    public KittyUnicodePlaceholderProtocol() {
    }

    @Override
    public void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException {
        if (area.isEmpty()) {
            return;
        }

        if (rawOutput == null) {
            throw new RuntimeIOException("Kitty unicode placeholder protocol requires raw output for initial transmission");
        }

        int imageId = NativeImageCache.imageId(area);

        // Only transmit the image data when the content or position has changed.
        // The virtual placement persists on the terminal side.
        List<Rect> stale = cache.staleAreasToClear(image, area, NativeImageCache.generationOf(rawOutput));
        if (stale != null) {
            // Delete any previously shown virtual placement whose footprint this one does not
            // fully cover, so a shrinking image (e.g. FILL -> FIT) does not leave the old one
            // behind. d=I deletes the image data; virtual placements require d=i/I (not d=a).
            for (Rect staleArea : stale) {
                String delete = String.format("\033_Ga=d,d=I,i=%d,q=2\033\\",
                    NativeImageCache.imageId(staleArea));
                rawOutput.write(delete.getBytes(StandardCharsets.US_ASCII));
            }

            String base64Data = cache.payload(image, () -> NativeImageCache.encodeBase64(image));
            transmitVirtualPlacement(rawOutput, base64Data, imageId, area.width(), area.height());
            rawOutput.flush();
        }

        // Always write placeholder cells into the buffer so the diff engine keeps them.
        // If the cells haven't changed, the diff produces nothing — zero terminal I/O.
        writePlaceholderCells(buffer, area, imageId);
    }

    /**
     * Transmits the image with a virtual placement ({@code U=1}) so it can be
     * displayed via unicode placeholder characters. The terminal stores the image
     * but does not display it until placeholder characters are emitted.
     */
    private void transmitVirtualPlacement(OutputStream out, String base64Data, int imageId,
                                          int cols, int rows) throws IOException {
        int offset = 0;
        int length = base64Data.length();
        boolean first = true;

        while (offset < length) {
            int chunkEnd = Math.min(offset + CHUNK_SIZE, length);
            String chunk = base64Data.substring(offset, chunkEnd);
            boolean more = chunkEnd < length;

            StringBuilder cmd = new StringBuilder();
            cmd.append(APC);

            if (first) {
                // a=T: transmit and create virtual placement
                // U=1: virtual placement (display via unicode placeholders)
                // f=100: PNG format
                // t=d: direct transmission
                // i=id: image id (encoded in fg color of placeholder chars)
                // q=2: suppress terminal reply
                // c=cols, r=rows: virtual placement size
                // m=0/1: more chunks
                cmd.append(String.format("a=T,U=1,f=100,t=d,i=%d,q=2,c=%d,r=%d,m=%d;",
                    imageId, cols, rows, more ? 1 : 0));
                first = false;
            } else {
                cmd.append(String.format("m=%d;", more ? 1 : 0));
            }

            cmd.append(chunk);
            cmd.append(ST);

            out.write(cmd.toString().getBytes(StandardCharsets.US_ASCII));
            offset = chunkEnd;
        }
    }

    /**
     * Writes unicode placeholder cells into the buffer.
     * <p>
     * Each cell contains the placeholder character ({@code U+10EEEE}) with combining
     * diacritics encoding the row and column position. The image id is encoded in the
     * foreground color as 24-bit RGB (lower 3 bytes of the integer id).
     * <p>
     * For columns after the first in each row, the diacritics are inherited from the
     * left neighbor per the Kitty spec, so we only need the row diacritic on the first
     * column cell. Subsequent cells in the same row use only the placeholder character.
     */
    private void writePlaceholderCells(Buffer buffer, Rect area, int imageId) {
        // Encode image id as 24-bit RGB foreground color (lower 3 bytes).
        // The most significant byte (4th diacritic) is added if the id exceeds 0xFFFFFF.
        int idR = (imageId >> 16) & 0xFF;
        int idG = (imageId >> 8) & 0xFF;
        int idB = imageId & 0xFF;
        Style fgStyle = Style.EMPTY.fg(new Color.Rgb(idR, idG, idB));

        int maxRow = Math.min(area.height(), MAX_DIACRITICS);
        int maxCol = Math.min(area.width(), MAX_DIACRITICS);
        int idExtra = (imageId >> 24) & 0xFF;

        for (int row = 0; row < maxRow; row++) {
            for (int col = 0; col < maxCol; col++) {
                StringBuilder symbol = new StringBuilder();
                symbol.append(PLACEHOLDER);

                if (col == 0) {
                    // First column: row diacritic + column diacritic (0)
                    symbol.append(DIACRITICS[row]);
                    symbol.append(DIACRITICS[col]);
                } else {
                    // Subsequent columns: only need the placeholder.
                    // Per the Kitty spec, if the previous cell has the same fg color
                    // and no diacritics, the row is inherited and column = prev + 1.
                    // But for correctness with all terminals, include row + col.
                    symbol.append(DIACRITICS[row]);
                    symbol.append(DIACRITICS[col]);
                }

                // Add the most-significant-byte diacritic if the id exceeds 24 bits.
                if (idExtra != 0) {
                    symbol.append(DIACRITICS[idExtra]);
                }

                Cell cell = new Cell(symbol.toString(), fgStyle);
                buffer.set(area.x() + col, area.y() + row, cell);
            }
        }
    }

    @Override
    public boolean requiresRawOutput() {
        return true;
    }

    @Override
    public boolean handlesOwnScaling() {
        return true;
    }

    @Override
    public Resolution resolution() {
        return new Resolution(8, 16);
    }

    @Override
    public String name() {
        return "Kitty (Unicode Placeholders)";
    }

    @Override
    public TerminalImageProtocol protocolType() {
        return TerminalImageProtocol.KITTY;
    }
}
