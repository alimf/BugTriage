package bugtriage;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class BugHistoryFetcher {
    static Map<String, org.w3c.dom.Document> stringDocumentMap = new HashMap<>();
    static String BUG_ROOT_PATH = "bugs";
    static String NODE_NAME_BUG_ID = "bug_id";
    static String NODE_NAME_TITLE = "title";
    static String NODE_NAME_PRODUCT = "product";
    static String NODE_NAME_COMPONENT = "component";
    static String NODE_NAME_ASSIGNEE = "assignee";
    static String ALL_PRODUCTS_LIST = "allProducts";

    public static void main(String[] args) {

        BugHistoryFetcher test = new BugHistoryFetcher();
        test.formatXmlFile();
        if (stringDocumentMap.isEmpty()) {
            return;
        }
        stringDocumentMap.forEach((key, value) -> test.writeXMLFile(value, key + "-" + "bug-list.xml"));


//        test.writeBugTitlesToFile("bugTitleList.txt");
//
//        test.getAllBugTitles("bugTitleList.txt").forEach(System.out::println);

    }


    private void writeBugTitlesToFile(String fileName) throws IOException {
        String titles = String.join("\n", getAllBugsTitlesByIds(readBugIDs()));

        Path path = Paths.get(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(titles);
        }
    }

    private List<String> getAllBugsTitlesByIds(List<String> bugIds) throws IOException {
        List<String> titles = new ArrayList<>();
        bugIds.forEach(bugId -> {
            titles.add(bugId + " " + getBugTitleByBugId(bugId));
        });
        return titles;
    }

    private List<String> readBugIDs() {
        String bugData;
        try {
            bugData = FileUtils.readFileToString(new File("bugIDList.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!StringUtil.isBlank(bugData)) {
            return Arrays.asList(bugData.split("\n"));
        }
        return Collections.emptyList();
    }

    private String getBugTitleByBugId(String bugId) {
        Document doc = null;
        try {
            doc = Jsoup.connect("https://bugs.eclipse.org/bugs/show_activity.cgi?id=" + bugId).get();
            System.out.println(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements newsHeadlines = doc.select("html body.bugs-eclipse-org-bugs.yui-skin-sam div#titles span#title");
        return newsHeadlines.html();
    }


    public void writeXMLFile(org.w3c.dom.Document doc, String fileName) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(BUG_ROOT_PATH + File.separator + fileName));
            configureTransformer(transformer);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        System.out.println("File saved!");
    }

    private void configureTransformer(Transformer transformer) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    public void formatXmlFile() {

        List<String> bugIDs = readBugIDs();
        if (bugIDs == null || bugIDs.isEmpty()) {
            return;
        }
        //System.out.println(((Element) bugs.item(i)).getElementsByTagName("bug_id").item(0).getTextContent());
        bugIDs.forEach(bugId -> {
            Map<String, String> bugData = fetchBugData(bugId);
            appendChild(getProductWiseRootElement(ALL_PRODUCTS_LIST), bugData);
            appendChild(getProductWiseRootElement(bugData.get(NODE_NAME_PRODUCT)), bugData);
        });
    }

    public Map<String, String> fetchBugData(String bugId) {

        Map<String, String> bugData = new HashMap<>();
        System.out.println("Current bug: " + bugId);
        Document doc = null;
        try {
            doc = Jsoup.connect("https://bugs.eclipse.org/bugs/show_bug.cgi?id=" + bugId).timeout(10 * 1000).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(doc.toString());
        //	Document doc = Jsoup.connect("https://netbeans.org/bugzilla/show_activity.cgi?id=" + bugs.get(i)).timeout(10 * 1000).get();
//			Elements newsHeadlines = doc.select("html body #bugzilla-body table tbody tr");
        String bugTitle = doc.select("html title").html();
        String bugProduct = doc.select("html body #bugzilla-body table tbody tr td #field_container_product").html();
        String bugComponent = doc.select("html body #bugzilla-body table tbody tr td #field_container_component").html();
        String bugAssignee = doc.select("html body #bugzilla-body table tbody tr td #field_label_assigned_to").next().select("span span").html();

        bugData.put(NODE_NAME_BUG_ID, bugId);
        bugData.put(NODE_NAME_TITLE, bugTitle);
        bugData.put(NODE_NAME_PRODUCT, bugProduct);
        bugData.put(NODE_NAME_COMPONENT, bugComponent);
        bugData.put(NODE_NAME_ASSIGNEE, bugAssignee);

        return bugData;
    }

    private void appendChild(org.w3c.dom.Document document, Map<String, String> data) {
        Element rootElement = document.getDocumentElement();
        Element bug = document.createElement("bug");
        Element id = document.createElement("bug_id");
        Element title = document.createElement("title");
        Element product = document.createElement("product");
        Element component = document.createElement("component");
        Element assignee = document.createElement("assignee");

        (id).appendChild(document.createTextNode(data.get(NODE_NAME_BUG_ID)));
        (bug).appendChild(id);
        title.appendChild(document.createTextNode(data.get(NODE_NAME_TITLE)));
        product.appendChild(document.createTextNode(data.get(NODE_NAME_PRODUCT)));
        component.appendChild(document.createTextNode(data.get(NODE_NAME_COMPONENT)));
        assignee.appendChild(document.createTextNode(data.get(NODE_NAME_ASSIGNEE)));

        bug.appendChild(title);
        bug.appendChild(product);
        bug.appendChild(component);
        bug.appendChild(assignee);
        rootElement.appendChild(bug);
    }

    private org.w3c.dom.Document getProductWiseRootElement(String product) {
        if (stringDocumentMap == null) {
            stringDocumentMap = new HashMap<>();
        }
        if (stringDocumentMap.isEmpty()) {
            stringDocumentMap.put(ALL_PRODUCTS_LIST, createDocument());
        }
        org.w3c.dom.Document document = stringDocumentMap.get(product);
        if (document == null) {
            document = createDocument();
            stringDocumentMap.put(product, document);
            return document;
        }
        return document;
    }

    private org.w3c.dom.Document createDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        org.w3c.dom.Document document = db.newDocument();
        Element rootElement = document.createElement("bugs");
        document.appendChild(rootElement);
        return document;
    }

}
