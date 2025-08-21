import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Fixes2 {
    private static List<String> extractPropertiesDefinitions(Element fxRoot) {
        Pattern propertyDefinition = Pattern.compile("DEFINE PROPERTY ([A-Za-z0-9]+)");
        NodeList childNodes = fxRoot.getChildNodes();
        List<String> properties = IntStream.range(0, childNodes.getLength())
                .mapToObj(childNodes::item)
                .filter(node -> node.getNodeType() == Node.COMMENT_NODE)
                .filter(node -> node.getTextContent()
                        .contains("DEFINE PROPERTY"))
                .map(node -> {
                    Matcher m = propertyDefinition.matcher(node.getTextContent());
                    if (m.find()) {
                        return Optional.of(m.group(1));
                    } else {
                        return Optional.<String>empty();
                    }
                })
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        return properties;
    }

    @Test
    void getProperties() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse("D:\\github-checkout\\Audio-Workshops\\midi-preset-manager\\src\\main\\java\\com\\hypercube\\mpm\\javafx\\widgets\\progress\\ProgressDialog.fxml");
        doc.getDocumentElement()
                .normalize();

        Element fxRoot = (Element) doc.getElementsByTagName("fx:root")
                .item(0);
        if (fxRoot != null && fxRoot.hasAttribute("type")) {

            List<String> properties = extractPropertiesDefinitions(fxRoot);
            properties.forEach(p -> System.out.println(p));
        }
    }
}
