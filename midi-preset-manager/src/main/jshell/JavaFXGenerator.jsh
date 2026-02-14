import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
This jShell Script generate the right Node class for all FXML using fx:root
This is required to be able to use SceneBuilder
*/
String baseDir = "./src/main/java";
String outputDir = "./target/generated-sources";
int exitCode = 0;
System.out.println("===========================================================================");
System.out.println("JavaFXGenerator: build widget from FXML files   ");
System.out.println("Running in directory: "+System.getProperty("user.dir"));
System.out.println("Input directory     : "+baseDir);
System.out.println("Output directory    : "+outputDir);
System.out.println("===========================================================================");

void writeJavaFile(Path directory, String fileName, String content, String className) throws IOException {
    String packagePath = className.substring(0, className.lastIndexOf(".")).replace(".", File.separator);
    Path packageDir = directory.resolve(packagePath);
    Files.createDirectories(packageDir);
    Path filePath = directory.resolve(fileName);
    try (FileWriter writer = new FileWriter(filePath.toFile())) {
        writer.write(content);
    }
}
String calculateClassName(Path filePath, String baseDir) {
    String relativePath = Paths.get(baseDir).relativize(filePath).toString();
    String fileName = filePath.getFileName().toString();
    String className = relativePath
            .replace(File.separator, ".")
            .replace(fileName, fileName.replace(".fxml", ""));
    return className;
}
List<String> extractPropertiesDefinitions(Element fxRoot) {
    Pattern propertyDefinition = Pattern.compile("DEFINE PROPERTY ([A-Za-z0-9><:.]+)");
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
List<String> extractImports(Document doc){
    List<String> imports = new ArrayList<>();
    NodeList children = doc.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
        Node node = children.item(i);

        if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            ProcessingInstruction pi = (ProcessingInstruction) node;

            if ("import".equals(pi.getTarget())) {
                imports.add(pi.getData().trim());
            }
        }
    }
    return imports;
}

String generateJavaCode(String className, String typeAttribute, List<String> javaFxImports, List<String> widgetProperties) {
        String packageName = className.substring(0, className.lastIndexOf("."));
        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
        String fxmlImports = javaFxImports.stream().map(i->"import %s;".formatted(i)).collect(Collectors.joining("\n"));
        String properties = widgetProperties.stream()
                .map(property -> {
                    System.out.println("Widget property: "+property);
                    String[] v = property.split(":");
                    String propertyName = v.length==2?v[1]:v[0];
                    String propertyType = v.length==2?v[0]:"String";
                    String parametricType = "";
                    String propertyInnerType = propertyType;
                    String camelPropertyName = propertyName.substring(0, 1)
                            .toUpperCase() + propertyName.substring(1);
                    if ( propertyType.contains(".")){
                        propertyInnerType = propertyType;
                        parametricType = "<"+propertyInnerType+">";
                        propertyType = "Object";
                    }
                    return """
                                // PROPERTY @@NAME@@ ------------------------------------------------
                                private final @@TYPE@@Property@@ParametricType@@ @@NAME@@ = new Simple@@TYPE@@Property@@ParametricType@@();
                                public @@INNER_TYPE@@ get@@CAMELNAME@@() {
                                    return @@NAME@@.get();
                                }
                                public void set@@CAMELNAME@@(@@INNER_TYPE@@ value) {
                                    this.@@NAME@@.set(value);
                                }
                                public @@TYPE@@Property@@ParametricType@@ @@NAME@@Property() {
                                    return @@NAME@@;
                                }
                            """.replace("@@NAME@@", propertyName)
                            .replace("@@TYPE@@", propertyType)
                            .replace("@@INNER_TYPE@@", propertyInnerType)
                            .replace("@@ParametricType@@", parametricType)
                            .replace("@@CAMELNAME@@", camelPropertyName);
                })
                .collect(Collectors.joining());

        String installPropertiesListeners = widgetProperties.stream()
                .map(property -> {
                    String[] v = property.split(":");
                    String propertyName = v.length==2?v[1]:v[0];
                    String propertyType = v.length==2?v[0]:"String";
                    if (propertyType.contains("javafx.event.EventHandler"))
                    {
                        String paramType = propertyType.replaceAll(".*<|>", "");
                        return """
                               \t\tpropertiesHelper.declareEventHandlerListener("@@NAME@@",@@NAME@@Property(),@@CLASS@@);
                               \t""".replace("@@NAME@@", propertyName)
                               .replace("@@CLASS@@", paramType+".class");
                    }
                    else if ( propertyType.contains(".")){
                       // SimpleObjectProperty
                       return """
                              \t\tpropertiesHelper.declareListener("@@NAME@@",@@NAME@@Property(),@@CLASS@@);
                              \t""".replace("@@NAME@@", propertyName)
                                                            .replace("@@CLASS@@", propertyType+".class");
                    }
                    else
                    {
                        // StringProperty and other simple types
                        return """
                               \t\tpropertiesHelper.declareListener("@@NAME@@",@@NAME@@Property());
                               \t""".replace("@@NAME@@", propertyName);
                    }
                })
                .collect(Collectors.joining());

        return """
                package @@PKG@@;

                import com.hypercube.util.javafx.controller.ControllerHelper;
                import com.hypercube.util.javafx.view.View;
                import com.hypercube.util.javafx.view.properties.PropertiesHelper;
                import javafx.scene.Scene;
                @@FXML_IMPORTS@@
                import javafx.beans.property.*;

                //
                // THIS FILE IS GENERATED, DON'T EDIT IT
                //
                public class @@CLS@@ extends @@SUPERCLS@@ implements View<@@CLS@@Controller> {
                    PropertiesHelper propertiesHelper = new PropertiesHelper(this);

                    public @@CLS@@() {
                        ControllerHelper.loadFXML(this);
                        if (ControllerHelper.isNonSceneBuilderLaunch()) {
                            propertiesHelper.installSceneObserver();
                    @@LISTENERS@@
                        }
                    }

                    @Override
                    public @@CLS@@Controller getCtrl() {
                        return ControllerHelper.getController(this);
                    }

                @@PROPS@@
                }
                """
                .replace("@@PKG@@", packageName)
                .replace("@@CLS@@",simpleClassName)
                .replace("@@SUPERCLS@@",typeAttribute)
                .replace("@@FXML_IMPORTS@@",fxmlImports)
                .replace("@@LISTENERS@@",installPropertiesListeners)
                .replace("@@PROPS@@", properties);
    }

{
    Files.createDirectories(Paths.get(outputDir));

    try (Stream<Path> paths = Files.walk(Paths.get(baseDir))) {
        paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".fxml"))
                .forEach(path -> {
                    try {
                        File fxmlFile = path.toFile();
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(fxmlFile);
                        doc.getDocumentElement().normalize();

                        List<String> javaFxImports = extractImports(doc);

                        Element fxRoot = (Element) doc.getElementsByTagName("fx:root").item(0);
                        if (fxRoot != null && fxRoot.hasAttribute("type")) {
                            String typeAttribute = fxRoot.getAttribute("type");
                            String className = calculateClassName(path, baseDir);

                            List<String> widgetProperties = extractPropertiesDefinitions(fxRoot);

                            var result = fxRoot.getElementsByTagName("fx:define");
                            if (result.getLength() > 0) {
                                Element fxDefine = (Element) result.item(0);
                                NodeList childNodes = fxDefine.getChildNodes();
                                for (int idx = 0; idx < childNodes.getLength(); idx++) {
                                    Node child = childNodes.item(idx);
                                    if (child instanceof Element element) {
                                        String id = element.getAttribute("fx:id");
                                        System.out.println("Widget property: " + id);
                                        widgetProperties.add(id);
                                    }
                                }
                            }

                            String javaCode = generateJavaCode(className, typeAttribute,javaFxImports,widgetProperties);
                            String relativePath = Paths.get(baseDir).relativize(path).getParent().toString();
                            if (!relativePath.isEmpty()) {
                                relativePath = relativePath + File.separator;
                            }
                            String javaFile = relativePath + className.substring(className.lastIndexOf(".") + 1) + ".java";
                            System.out.println("Generate: " + javaFile);
                            writeJavaFile(Paths.get(outputDir), javaFile, javaCode, className);
                        } else {
                            throw new RuntimeException("File: " + path + ", fx:root tag or 'type' attribute not found.");
                        }
                    } catch (ParserConfigurationException | SAXException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    } catch (Exception e) {
        System.err.println("Error during generation: " + e.toString());
        exitCode = -1;
    }
}
/exit exitCode;
