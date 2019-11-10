package bugtriage;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BugHistoryFetcher {
    static org.w3c.dom.Document dom;

    public static void main(String[] args) throws IOException, TransformerException, ParserConfigurationException {

        BugHistoryFetcher test = new BugHistoryFetcher();
        test.formatXmlFile();
        test.writeXMLFile(dom, "bug-list.xml");
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
            titles.add(bugId + " "+ getBugTitleByBugId(bugId));
        });
        return titles;
    }

    private ArrayList<String> readBugIDs() throws IOException {
        String bugData = FileUtils.readFileToString(new File("bugIDList.txt"));
        String[] bugs = bugData.split("\n");

        System.out.println(bugs.length);

        ArrayList<String> idList = new ArrayList<>(Arrays.asList(bugs));
        return idList;
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


    private List<String> getAllBugTitles(String pathName) {
        try {
            return Files.readAllLines(Paths.get(pathName));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    public void writeXMLFile(org.w3c.dom.Document doc, String fileName) throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileName));
        configureTransformer(transformer);
        transformer.transform(source, result);

        System.out.println("File saved!");
    }

    /**
     * Configure XML transformer for indentation
     *
     * @param transformer xml Transformer from {@link #printXMLtoConsole(org.w3c.dom.Document)} and {@link #writeXMLFile(org.w3c.dom.Document, String)}
     */
    private void configureTransformer(Transformer transformer) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    public void formatXmlFile() throws ParserConfigurationException, IOException {

        ArrayList<String> bugs = readBugIDs();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.newDocument();
        Element rootEle = dom.createElement("bugs");
        dom.appendChild(rootEle);


        for (int i = 0; i < bugs.size(); i++) {
            //System.out.println(((Element) bugs.item(i)).getElementsByTagName("bug_id").item(0).getTextContent());
            System.out.println("Current bug: " + bugs.get(i));
            Document doc = Jsoup.connect("https://bugs.eclipse.org/bugs/show_bug.cgi?id=" + bugs.get(i)).timeout(10 * 1000).get();
            System.out.println(doc.toString());
            //	Document doc = Jsoup.connect("https://netbeans.org/bugzilla/show_activity.cgi?id=" + bugs.get(i)).timeout(10 * 1000).get();
//			Elements newsHeadlines = doc.select("html body #bugzilla-body table tbody tr");
            String bugTitle = doc.select("html title").html();
            String bugProduct = doc.select("html body #bugzilla-body table tbody tr td #field_container_product").html();
            String bugComponent = doc.select("html body #bugzilla-body table tbody tr td #field_container_component").html();
            String bugAssginee = doc.select("html body #bugzilla-body table tbody tr td #field_label_assigned_to").next().select("span span").html();
//
            Element bug = dom.createElement("bug");
            Element id = dom.createElement("bug_id");
            Element title = dom.createElement("title");
            Element product = dom.createElement("product");
            Element component = dom.createElement("component");
            Element assignee = dom.createElement("assignee");

            (id).appendChild(dom.createTextNode(bugs.get(i)));
            (bug).appendChild(id);
            title.appendChild(dom.createTextNode(bugTitle));
            product.appendChild(dom.createTextNode(bugProduct));
            component.appendChild(dom.createTextNode(bugComponent));
            assignee.appendChild(dom.createTextNode(bugAssginee));

            bug.appendChild(title);
            bug.appendChild(product);
            bug.appendChild(component);
            bug.appendChild(assignee);
            rootEle.appendChild(bug);
        }
    }

}
