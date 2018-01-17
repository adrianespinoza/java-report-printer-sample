package com.dharbor.providerportal.framework.printing;

import java.util.HashMap;
import com.itextpdf.text.pdf.PdfPageEventHelper;

public class HeaderFooterPageEvent extends PdfPageEventHelper
{

    public HeaderFooterPageEvent(String packageName, String actualDate, boolean b)
    {
    }

    public HashMap<String, Integer> index = new HashMap<String, Integer>();

}
