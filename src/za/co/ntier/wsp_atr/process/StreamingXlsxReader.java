package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

/**
 * Streaming reader for large XLSX files. Uses POI's OPC + XSSFReader for
 * package navigation and StAX for per-sheet XML parsing — memory usage stays
 * proportional to the shared-strings table (unique strings only) plus a single
 * row's worth of cells, never the whole workbook.
 *
 * <p>Designed as a drop-in replacement for code that previously held a
 * {@code Workbook} and iterated sheet/row/cell. Callers receive each row as a
 * {@code Map<Integer, String>} of column index → formatted text. Empty cells
 * are omitted from the map; cell text is never trimmed by the reader.</p>
 *
 * <p>Rows missing entirely from the XML (XLSX gaps) are still emitted to the
 * consumer as empty maps, so empty-row counting works the same as with the
 * DOM API.</p>
 */
public class StreamingXlsxReader implements AutoCloseable {

    /** Callback receiving each row in stream order. */
    public interface RowConsumer {
        Action accept(int rowIdx0Based, Map<Integer, String> cells) throws Exception;
    }

    public enum Action { CONTINUE, STOP }

    private static final XMLInputFactory XML_FACTORY;
    static {
        XML_FACTORY = XMLInputFactory.newInstance();
        XML_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    private final OPCPackage pkg;
    private final XSSFReader reader;
    private final ReadOnlySharedStringsTable sharedStrings;
    private final StylesTable styles;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final List<String> sheetNames;

    public StreamingXlsxReader(File file) throws Exception {
        this.pkg = OPCPackage.open(file, PackageAccess.READ);
        this.reader = new XSSFReader(pkg);
        this.sharedStrings = new ReadOnlySharedStringsTable(pkg);
        this.styles = reader.getStylesTable();
        this.sheetNames = collectSheetNames();
    }

    public List<String> getSheetNames() {
        return Collections.unmodifiableList(sheetNames);
    }

    public boolean hasSheet(String name) {
        return sheetNames.contains(name);
    }

    /**
     * Stream the rows of a sheet. Each row at index ≥ {@code startRow0Based}
     * is delivered to the consumer. Rows missing from the XML (gap rows) are
     * delivered as empty maps so the consumer's empty-row counter sees them.
     *
     * @param sheetName       name of the worksheet to stream.
     * @param startRow0Based  first row index (0-based) to deliver. Rows before
     *                        this are still parsed but not delivered.
     * @param wantedColumns   if non-null, only cells whose 0-based column index
     *                        is in this set are kept (memory optimisation).
     *                        Pass {@code null} to keep every cell.
     * @param consumer        called for each row. Return {@link Action#STOP}
     *                        to abort streaming.
     */
    public void streamSheet(String sheetName,
                            int startRow0Based,
                            Set<Integer> wantedColumns,
                            RowConsumer consumer) throws Exception {
        InputStream sheetStream = openSheetStream(sheetName);
        if (sheetStream == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }
        try {
            parseSheet(sheetStream, startRow0Based, wantedColumns, consumer);
        } finally {
            sheetStream.close();
        }
    }

    @Override
    public void close() throws IOException {
        pkg.close();
    }

    // ---------- internals ----------

    private List<String> collectSheetNames() throws Exception {
        List<String> names = new ArrayList<>();
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
        while (iter.hasNext()) {
            try (InputStream is = iter.next()) {
                names.add(iter.getSheetName());
            }
        }
        return names;
    }

    private InputStream openSheetStream(String name) throws Exception {
        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
        while (iter.hasNext()) {
            InputStream is = iter.next();
            if (name.equals(iter.getSheetName())) {
                return is;
            }
            is.close();
        }
        return null;
    }

    private void parseSheet(InputStream sheetStream,
                            int startRow,
                            Set<Integer> wanted,
                            RowConsumer consumer) throws Exception {
        XMLStreamReader xml = XML_FACTORY.createXMLStreamReader(sheetStream);
        try {
            int currentRow = -1;
            boolean rowSkipped = false;
            int expectedRow = startRow;
            Map<Integer, String> cells = new HashMap<>();

            boolean inCell = false;
            int curColIdx = -1;
            String cellType = null;
            int cellStyleIdx = -1;
            boolean inV = false;
            boolean inIs = false;
            boolean inT = false;
            StringBuilder cellText = new StringBuilder();
            Action stopFlag = Action.CONTINUE;

            while (xml.hasNext() && stopFlag == Action.CONTINUE) {
                int evt = xml.next();

                if (evt == XMLStreamConstants.START_ELEMENT) {
                    String name = xml.getLocalName();

                    if ("row".equals(name)) {
                        String r = xml.getAttributeValue(null, "r");
                        currentRow = (r != null) ? Integer.parseInt(r) - 1 : currentRow + 1;
                        rowSkipped = currentRow < startRow;
                        cells.clear();
                        // Defensive reset of cell-level flags.
                        inCell = false;
                        inV = inIs = inT = false;

                    } else if ("c".equals(name) && !rowSkipped) {
                        inCell = true;
                        String ref = xml.getAttributeValue(null, "r");
                        curColIdx = colFromRef(ref);
                        cellType = xml.getAttributeValue(null, "t");
                        String s = xml.getAttributeValue(null, "s");
                        cellStyleIdx = (s != null) ? Integer.parseInt(s) : -1;
                        cellText.setLength(0);
                        inV = inIs = inT = false;

                    } else if (inCell) {
                        if ("v".equals(name)) {
                            inV = true;
                        } else if ("is".equals(name)) {
                            inIs = true;
                        } else if ("t".equals(name) && inIs) {
                            inT = true;
                        }
                    }

                } else if ((evt == XMLStreamConstants.CHARACTERS
                            || evt == XMLStreamConstants.CDATA)
                           && (inV || inT)) {
                    cellText.append(xml.getText());

                } else if (evt == XMLStreamConstants.END_ELEMENT) {
                    String name = xml.getLocalName();

                    if ("v".equals(name)) {
                        inV = false;
                    } else if ("t".equals(name)) {
                        inT = false;
                    } else if ("is".equals(name)) {
                        inIs = false;
                    } else if ("c".equals(name) && inCell) {
                        inCell = false;
                        if (curColIdx >= 0
                                && (wanted == null || wanted.contains(curColIdx))) {
                            String text = resolveCellText(cellText.toString(), cellType, cellStyleIdx);
                            if (text != null && !text.isEmpty()) {
                                cells.put(curColIdx, text);
                            }
                        }
                    } else if ("row".equals(name) && !rowSkipped) {
                        // Fill XLSX row gaps with empty maps so the consumer's
                        // empty-row counter behaves like the DOM API.
                        while (expectedRow < currentRow && stopFlag == Action.CONTINUE) {
                            stopFlag = consumer.accept(expectedRow, Collections.<Integer, String>emptyMap());
                            expectedRow++;
                        }
                        if (stopFlag == Action.CONTINUE) {
                            stopFlag = consumer.accept(currentRow, cells);
                            expectedRow = currentRow + 1;
                        }
                    } else if ("sheetData".equals(name)) {
                        break;
                    }
                }
            }
        } finally {
            xml.close();
        }
    }

    /** "B12" → 1, "AA3" → 26, etc. Returns -1 for null/empty. */
    private static int colFromRef(String ref) {
        if (ref == null || ref.isEmpty()) {
            return -1;
        }
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                col = col * 26 + (c - 'A' + 1);
            } else if (c >= 'a' && c <= 'z') {
                col = col * 26 + (c - 'a' + 1);
            } else {
                break;
            }
        }
        return col - 1;
    }

    private String resolveCellText(String raw, String type, int styleIdx) {
        if (raw == null) {
            return "";
        }
        // Shared string: <v>idx</v>
        if ("s".equals(type)) {
            if (raw.isEmpty()) {
                return "";
            }
            try {
                int idx = Integer.parseInt(raw.trim());
                RichTextString rts = sharedStrings.getItemAt(idx);
                return rts == null ? "" : rts.toString();
            } catch (Exception e) {
                return "";
            }
        }
        // Boolean: 0/1
        if ("b".equals(type)) {
            return "0".equals(raw.trim()) ? "FALSE" : "TRUE";
        }
        // Inline string, formula-string result, error string — pass through.
        if ("inlineStr".equals(type) || "str".equals(type) || "e".equals(type)) {
            return raw;
        }
        // Default: numeric. Apply style-driven format (handles dates).
        if (raw.isEmpty()) {
            return "";
        }
        short fmtId = 0;
        String fmtStr = "General";
        if (styleIdx >= 0 && styles != null) {
            try {
                XSSFCellStyle style = styles.getStyleAt(styleIdx);
                if (style != null) {
                    fmtId = style.getDataFormat();
                    String s = style.getDataFormatString();
                    if (s != null && !s.isEmpty()) {
                        fmtStr = s;
                    }
                }
            } catch (Exception ignore) {
                // fall through with defaults
            }
        }
        try {
            double d = Double.parseDouble(raw.trim());
            return dataFormatter.formatRawCellContents(d, fmtId, fmtStr);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
