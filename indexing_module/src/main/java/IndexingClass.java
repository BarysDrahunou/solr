import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;


public class IndexingClass {

    public static void main(String[] args) {

        String urlString = "http://localhost:8983/solr/solr-homework";
        SolrClient solr = new HttpSolrClient.Builder(urlString).build();

        try {

            File[] files = new File("C://books").listFiles();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            if (files != null) {

                for (File file : files) {
                    Book book = BookBuilder.getBook(file, docBuilder);
                    System.out.println(book.getBookName());
                    solr.addBean(book);
                }
            }

            //Saving the changes
            solr.commit();

            System.out.println("Documents added");
        } catch (IOException | ParserConfigurationException | SolrServerException | SAXException e) {
            e.printStackTrace();
        }
    }
}
