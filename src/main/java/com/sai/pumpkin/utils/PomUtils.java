package com.sai.pumpkin.utils;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

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
            String gid = (String) xPath.compile("project/groupId").evaluate(doc, XPathConstants.STRING);
            String aid = (String) xPath.compile("project/artifactId").evaluate(doc, XPathConstants.STRING);
            String version = (String) xPath.compile("project/version").evaluate(doc, XPathConstants.STRING);
            return new String[]{gid, aid, version};
        } catch (Exception ex) {
            return new String[]{"", "", ""};
        }
    }

    public static void mains(String[] args) throws Exception{
        String pomContents = FileUtils.readFileToString(new File("/Users/saipkri/pumpkin_ws/wireless/rfm/pom.xml"));
        System.out.println(Arrays.deepToString(gidAidVersionArray(pomContents)));
    }
}
