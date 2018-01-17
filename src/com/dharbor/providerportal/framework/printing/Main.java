package com.dharbor.providerportal.framework.printing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main
{
    public static void main(String[] args)
    {
        List<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();
        
        Map<String, Object> contentMap = new HashMap<String, Object>();
        
        contentMap.put("content", "This is a content");
        contentMap.put("formName", "this is a form name");
        contentMap.put("subFormName", "Checklist");
        contentMap.put("sectionName", "this is a section name");
        contentMap.put("tittle", "this is a tittle");

        contentMap.put("isForm", true);
        contentMap.put("isSubForm", true);
        contentMap.put("isSection", false);
        contentMap.put("commExp", false);
        
        templates.add(contentMap);
        
        HtmlPDFPrinter printer = new HtmlPDFPrinter();
        
        printer.setRoot("E:\\005 EclipceWorkspaceMars\\ReportPrinter\\output\\");
        
        printer.print(templates, "mycustompackage", "12-02-2016", "12-02-2017", "legal name", "Track");
    }
}
