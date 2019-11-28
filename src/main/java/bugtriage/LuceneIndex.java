package bugtriage;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;

public class LuceneIndex {

    public void createIndex(Directory index) throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(index, config);
        addDocToIndex(w);
        w.close();
    }

    private void addDocToIndex(IndexWriter w) {

        try {
            File file = new File("bug-list.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            NodeList nodeList = doc.getElementsByTagName("bug");
            for (int itr = 0; itr < nodeList.getLength(); itr++) {
                Node node = nodeList.item(itr);
                System.out.println("\nNode Name :" + node.getNodeName());
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;

                    Document indexDoc = new Document();
                    indexDoc.add(new StringField("bugId", eElement.getElementsByTagName("bug_id").item(0).getTextContent(), Field.Store.YES));
                    indexDoc.add(new TextField("title", eElement.getElementsByTagName("title").item(0).getTextContent(), Field.Store.YES));
                    indexDoc.add(new TextField("product", eElement.getElementsByTagName("product").item(0).getTextContent(), Field.Store.YES));
                    indexDoc.add(new TextField("component", eElement.getElementsByTagName("component").item(0).getTextContent(), Field.Store.YES));
                    indexDoc.add(new TextField("assignee", eElement.getElementsByTagName("assignee").item(0).getTextContent(), Field.Store.YES));
                    System.out.println(indexDoc);
                    w.addDocument(indexDoc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
