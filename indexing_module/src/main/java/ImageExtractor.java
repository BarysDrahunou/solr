import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class ImageExtractor {
    public static String extractImage(File file, DocumentBuilder docBuilder) throws IOException, SAXException {

        Document document = docBuilder.parse(file.getPath());
        Node node = document.getElementsByTagName("binary").item(0);
        String pathToImage;

        if (node != null) {
            byte[] byteArray = Base64.getMimeDecoder().decode(node.getTextContent());

            ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
            BufferedImage bImage2 = ImageIO.read(bis);
            pathToImage = "searching_module/src/main/resources/static/images/" + FilenameUtils.removeExtension(file.getName()) + ".jpg";
            ImageIO.write(bImage2, "jpg", new File(pathToImage));
            return FilenameUtils.removeExtension(file.getName());
        }

        return null;
    }
}
