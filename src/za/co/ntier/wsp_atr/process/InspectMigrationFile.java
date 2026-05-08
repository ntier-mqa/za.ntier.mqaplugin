package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.InspectMigrationFile")
public class InspectMigrationFile extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final int MAX_CONSECUTIVE_EMPTY = 10;

    private static final XMLInputFactory XML_FACTORY;
    static {
        XML_FACTORY = XMLInputFactory.newInstance();
        XML_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XML_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found: " + BULK_UPLOAD_PATH);
        }

        int totalTabs = 0;

        try (OPCPackage pkg = OPCPackage.open(file)) {
            XSSFReader reader = new XSSFReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    String tabName = sheets.getSheetName();
                    int[] counts = countSheet(sheetStream); // [headerCount, recordCount]
                    addLog("Tab: " + tabName
                            + "  |  Headers: " + counts[0]
                            + "  |  Records: " + counts[1]);
                    totalTabs++;
                }
            }
        }

        return "Inspected " + totalTabs + " tab(s).";
    }

    /**
     * Streams the sheet XML with StAX and returns [headerCount, recordCount].
     * Row 1 (r=1) is treated as the header row. Data rows start at r=2.
     * Stops counting after MAX_CONSECUTIVE_EMPTY consecutive empty data rows.
     */
    private static int[] countSheet(InputStream sheetStream) throws Exception {
        int headerCount = 0;
        int recordCount = 0;
        int consecutiveEmpty = 0;
        boolean done = false;

        int currentRowIdx = -1;
        boolean currentRowHasData = false;
        boolean inCell = false;
        boolean isSharedString = false;
        boolean inValue = false;
        boolean inInlineStr = false;
        StringBuilder cellText = new StringBuilder();

        XMLStreamReader xml = XML_FACTORY.createXMLStreamReader(sheetStream);
        try {
            while (xml.hasNext()) {
                int event = xml.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = xml.getLocalName();

                    if ("row".equals(name)) {
                        String r = xml.getAttributeValue(null, "r");
                        currentRowIdx = r != null ? Integer.parseInt(r) - 1 : currentRowIdx + 1;
                        currentRowHasData = false;

                    } else if ("c".equals(name)) {
                        inCell = true;
                        isSharedString = "s".equals(xml.getAttributeValue(null, "t"));
                        cellText.setLength(0);

                    } else if (inCell && "v".equals(name)) {
                        inValue = true;

                    } else if (inCell && "t".equals(name)) {
                        inInlineStr = true;
                    }

                } else if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)
                        && (inValue || inInlineStr)) {
                    cellText.append(xml.getText());

                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String name = xml.getLocalName();

                    if ("v".equals(name)) {
                        inValue = false;

                    } else if ("t".equals(name)) {
                        inInlineStr = false;

                    } else if ("c".equals(name)) {
                        inCell = false;
                        // Shared-string index is always non-empty; numeric/string values checked by content.
                        boolean hasContent = isSharedString || cellText.length() > 0;
                        if (hasContent) {
                            if (currentRowIdx == 0) {
                                headerCount++;
                            } else {
                                currentRowHasData = true;
                            }
                        }

                    } else if ("row".equals(name) && currentRowIdx > 0) {
                        if (currentRowHasData) {
                            consecutiveEmpty = 0;
                            recordCount++;
                        } else {
                            if (++consecutiveEmpty > MAX_CONSECUTIVE_EMPTY) {
                                done = true;
                            }
                        }
                    } else if ("sheetData".equals(name)) {
                        done = true;
                    }

                    if (done) {
                        break;
                    }
                }
            }
        } finally {
            xml.close();
        }

        return new int[]{headerCount, recordCount};
    }
}
