package bugtriage;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

public class BugTriageTest {
    public static void main(String[] args) {
        LuceneIndex luceneIndex = new LuceneIndex();
        SearchIndex searchIndex = new SearchIndex();
        Directory directory = new RAMDirectory();

        String query = "label when there is just one elemen";

        try {
            luceneIndex.createIndex(directory);
            searchIndex.search(directory, query);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
