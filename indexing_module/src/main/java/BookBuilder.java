import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class BookBuilder {

    public static Book getBook(File file, DocumentBuilder docBuilder) throws IOException, SAXException {

        Document document = docBuilder.parse(file.getPath());
        String bookFileName = ImageExtractor.extractImage(file, docBuilder);

        return new Book.Builder()
                .withGenre(getGenre(document))
                .withAuthorName(getAuthorName(document))
                .withAuthorSurname(getAuthorSurname(document))
                .withBookName(getTitle(document))
                .withAnnotation(getAnnotation(document))
                .withDate(getDate(document))
                .withLanguage(getLanguage(document))
                .withId(getId(document))
                .withVersion(getVersion(document))
                .withText(getText(document))
                .withRawText(getRawText(document))
                .withBookFileName(bookFileName)
                .build();
    }

    private static String getGenre(Document document) {
        return getSimpleNode("genre", document);
    }

    private static String getAuthorName(Document document) {
        String result = "";
        String firstName = getSimpleNode("first-name", document);
        String middleName = getSimpleNode("middle-name", document);
        if (firstName != null) {
            result = result.concat(firstName);
        }
        if (middleName != null) {
            result = result.concat(" ").concat(middleName);
        }
        return result.trim();
    }

    private static String getAuthorSurname(Document document) {
        return getSimpleNode("last-name", document);
    }

    private static String getTitle(Document document) {
        return getSimpleNode("book-title", document);
    }

    private static String getAnnotation(Document document) {
        return getSimpleNode("annotation", document);
    }

    private static String getDate(Document document) {
        NodeList nodeList = document.getElementsByTagName("date");

        for (int i = 0; i < nodeList.getLength(); i++) {
            String date = nodeList.item(i).getTextContent();
            if (date != null) {

                List<String> dates = new ArrayList<>(Arrays.asList(date.split(" ")));

                dates.addAll(Arrays.asList(date.split("-")));
                dates.addAll(Arrays.asList(date.split("/")));
                dates.addAll(Arrays.asList(date.split("\\.")));

                for (String elem : dates) {

                    if (elem.matches("^\\d{4}$")) {
                        return elem;
                    }
                }
            }
        }
        return null;
    }

    private static String getLanguage(Document document) {
        return getSimpleNode("lang", document);
    }

    private static String getId(Document document) {
        return getSimpleNode("id", document);
    }

    private static String getVersion(Document document) {
        return getSimpleNode("version", document);
    }

    private static String getText(Document document) {
        List<String> list = new ArrayList<>();

        NodeList bodyList = document.getElementsByTagName("body");

        for (int i = 0; i < bodyList.getLength(); i++) {

            NodeList nodeList = ((Element) bodyList.item(0)).getElementsByTagName("p");

            for (int j = 0; j < nodeList.getLength(); j++) {
                String textContent = nodeList.item(j).getTextContent();

                if (textContent != null) {
                    list.add(textContent.replaceAll("\\s+", " ").trim());
                }
            }
        }

        return !list.isEmpty() ? String.join(" ", list) : null;
    }

    private static String getRawText(Document document) {
        return document.getElementsByTagName("body").item(0).getTextContent();
    }

    private static String getSimpleNode(String nodeName, Document document) {
        Node node = document.getElementsByTagName(nodeName).item(0);
        return node != null ? node.getTextContent().replaceAll("\\s+", " ").trim() : null;
    }
}