import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

String generateJavaCode(String className, String typeAttribute) {
    String packageName = className.substring(0, className.lastIndexOf("."));
    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);

    return String.format("""
            package %s;

            import com.hypercube.util.javafx.controller.ControllerHelper;
            import com.hypercube.util.javafx.view.View;
            import javafx.scene.layout.%s;

            //
            // THIS FILE IS GENERATED, DON'T EDIT IT
            //
            public class %s extends %s implements View<%sController> {
                public %s() {
                    ControllerHelper.loadFXML(this);
                }

                @Override
                public %sController getController() {
                    return ControllerHelper.getController(this);
                }
            }
            """, packageName, typeAttribute, simpleClassName, typeAttribute, simpleClassName, simpleClassName, simpleClassName);
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

                        Element fxRoot = (Element) doc.getElementsByTagName("fx:root").item(0);
                        if (fxRoot != null && fxRoot.hasAttribute("type")) {
                            String typeAttribute = fxRoot.getAttribute("type");
                            String className = calculateClassName(path, baseDir);
                            String javaCode = generateJavaCode(className, typeAttribute);
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
