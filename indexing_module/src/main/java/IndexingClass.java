import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;


public class IndexingClass {

    public static void main(String[] args) {

        String urlString = "http://localhost:8983/solr/solr-homework";
        SolrClient solr = new HttpSolrClient.Builder(urlString).build();

        //Preparing the Solr document
        SolrInputDocument doc = new SolrInputDocument();

        try {

            File[] files = new File("/books").listFiles();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            if (files != null) {
                for (File file : files) {
                    Book book = BookBuilder.getBook(file, docBuilder);
                    solr.addBean(book);
                }
            }

            doc.addField("id", "random-id");
            //Adding the document to Solr
            solr.add(doc);

            //Saving the changes
            solr.commit();

            System.out.println("Documents added");
        } catch (IOException | ParserConfigurationException | SolrServerException | SAXException e) {
            e.printStackTrace();
        }
    }
}
