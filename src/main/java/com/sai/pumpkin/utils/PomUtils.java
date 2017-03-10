package com.sai.pumpkin.utils;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;

/**
 * Created by saipkri on 07/03/17.
 */
public class PomUtils {

    public static String[] gidAidVersionArray(String pomContents) {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(
                    pomContents.getBytes());
            Document doc = builder.parse(input);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String gid = (String) xPath.compile("//groupId").evaluate(doc, XPathConstants.STRING);
            String aid = (String) xPath.compile("//artifactId").evaluate(doc, XPathConstants.STRING);
            String version = (String) xPath.compile("//version").evaluate(doc, XPathConstants.STRING);
            return new String[]{gid, aid, version};
        } catch (Exception ex) {
            ex.printStackTrace();
            return new String[]{"", "", ""};
        }
    }
}
