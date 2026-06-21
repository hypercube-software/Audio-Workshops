/*
This jShell Script generate a PUML diagram from a root class. It has been vibe coded with Gemini
*/
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

System.out.println("Classpath: " + System.getProperty("jshell.classpath"));

int exitCode = 0;

String jshellClasspath = System.getProperty("jshell.classpath");
if (jshellClasspath == null || jshellClasspath.isEmpty()) {
    System.err.println("Error: 'jshell.classpath' system property not set.");
    exitCode = 1;
    System.exit(exitCode);
}

List<URL> classpathUrls = new ArrayList<>();
for (String path : jshellClasspath.split(File.pathSeparator)) {
    try {
        classpathUrls.add(new File(path).toURI().toURL());
    } catch (java.net.MalformedURLException e) {
        System.err.println("Warning: Malformed URL for classpath entry: " + path + " - " + e.getMessage());
    }
}
var customClassLoader = new URLClassLoader(classpathUrls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());

System.out.println("Classpath used by URLClassLoader: " + jshellClasspath);

var classesToDiagram = new HashSet<Class<?>>();
var classLoader = customClassLoader;
String rootPackage = null;

// Check if class belongs to root package hierarchy
boolean isRootPackage(Class<?> clazz) {
    if (rootPackage == null) return true;
    return clazz.getName().startsWith(rootPackage);
}

// Add classes to diagram starting from root classes, recursively
void addClassesRecursive(Class<?> clazz) {
    if (clazz == null || clazz == Object.class || clazz.isPrimitive() || classesToDiagram.contains(clazz)) {
        return;
    }
    
    // Use rootPackage filter if set
    if (!isRootPackage(clazz)) {
        return;
    }

    // Filter out anonymous, synthetic, etc.
    if (clazz.isAnonymousClass() || clazz.isSynthetic() || clazz.isInterface() || clazz.isAnnotation()) {
        return;
    }
    
    classesToDiagram.add(clazz);
    
    // Add fields
    for (Field field : clazz.getDeclaredFields()) {
        addClassesRecursive(field.getType());
        // Handle collections
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            for (Type typeArg : ((ParameterizedType) genericType).getActualTypeArguments()) {
                if (typeArg instanceof Class) {
                    addClassesRecursive((Class<?>) typeArg);
                }
            }
        }
    }
    // Add superclass
    addClassesRecursive(clazz.getSuperclass());
}

// Find classes in package
List<Class<?>> getClassesInPackage(String packageName) {
    List<Class<?>> classes = new ArrayList<>();
    String path = packageName.replace('.', '/');
    
    for (URL url : classpathUrls) {
        File file = new File(url.getFile());
        if (file.isDirectory()) {
            File packageDir = new File(file, path);
            if (packageDir.exists()) {
                scanDirectory(packageDir, packageName, classes);
            }
        } else if (file.getName().endsWith(".jar")) {
            scanJar(file, packageName, classes);
        }
    }
    return classes;
}

void scanDirectory(File dir, String packageName, List<Class<?>> classes) {
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File file : files) {
        if (file.isDirectory()) {
            scanDirectory(file, packageName + "." + file.getName(), classes);
        } else if (file.getName().endsWith(".class")) {
            try {
                classes.add(Class.forName(packageName + "." + file.getName().substring(0, file.getName().length() - 6), false, classLoader));
            } catch (Exception e) {}
        }
    }
}

void scanJar(File jarFile, String packageName, List<Class<?>> classes) {
    try (JarFile jar = new JarFile(jarFile)) {
        Enumeration<JarEntry> entries = jar.entries();
        String path = packageName.replace('.', '/');
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(path) && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (Exception e) {}
            }
        }
    } catch (Exception e) {}
}

{
    var rootClasses = System.getProperty("root.classes");

    if (rootClasses == null || rootClasses.isEmpty()) {
        System.err.println("Usage: jshell -J-Droot.classes=com.example.Class1,com.example.Class2 generate_uml.jsh");
        exitCode = 1;
        System.exit(exitCode);
    }

    // Initialize diagram classes from root classes and their subclasses
    // Initialize with provided root classes
    for (String className : rootClasses.split(",")) {
        try {
            Class<?> rootClass = Class.forName(className.trim(), false, classLoader);
            addClassesRecursive(rootClass);
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Could not load class: " + className + " - " + e.getMessage());
        }
    }

    // Set rootPackage based on the first root class (as a reasonable heuristic for the project structure)
    try {
        Class<?> firstRootClass = Class.forName(rootClasses.split(",")[0].trim(), false, classLoader);
        String pkg = firstRootClass.getPackage().getName();
        rootPackage = pkg.substring(0, pkg.lastIndexOf('.')); // Package hierarchy one level above
        System.out.println("Root package set to: " + rootPackage);
    } catch (ClassNotFoundException e) {}

    // Transitive subclass discovery
    boolean added;
    do {
        added = false;
        List<Class<?>> allPotentialClasses = getClassesInPackage(rootPackage);
        for (Class<?> clazz : allPotentialClasses) {
            if (!classesToDiagram.contains(clazz)) {
                // Check if it's a subclass of ANY class already in the diagram
                for (Class<?> diagrammedClass : new ArrayList<>(classesToDiagram)) {
                    if (diagrammedClass.isAssignableFrom(clazz) && !diagrammedClass.equals(clazz)) {
                        System.out.println("Found transitive subclass: " + clazz.getSimpleName() + " of " + diagrammedClass.getSimpleName());
                        addClassesRecursive(clazz);
                        added = true;
                        break;
                    }
                }
            }
        }
    } while (added);


    if (!classesToDiagram.isEmpty()) {
        var plantUmlContent = new StringBuilder();
        plantUmlContent.append("@startuml\n");
        plantUmlContent.append("skinparam fontSize 14\n");
        plantUmlContent.append("skinparam classFontSize 16\n");
        plantUmlContent.append("skinparam classAttributeIconSize 0\n");
        plantUmlContent.append("skinparam classArrowColor #AAAAAA\n");
        plantUmlContent.append("skinparam classBorderColor #666666\n");
        plantUmlContent.append("skinparam classBackgroundColor #EEEEEE\n");

        // ... (The rest of the logic remains largely the same, just adjust class name resolution)
        
        var classInfoMap = new java.util.HashMap<String, List<String>>();
        var relationships = new java.util.HashSet<String>();

        for (Class<?> clazz : classesToDiagram) {
            var fieldsContent = new java.util.ArrayList<String>();
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String accessModifier = Modifier.isProtected(field.getModifiers()) ? "#" : Modifier.isPrivate(field.getModifiers()) ? "-" : "+";
                fieldsContent.add(accessModifier + " " + field.getType().getSimpleName() + " " + field.getName());

                // Relationship inference with root package filtering
                Class<?> fieldType = field.getType();
                if (classesToDiagram.contains(fieldType) && isRootPackage(fieldType)) {
                    relationships.add(clazz.getSimpleName() + " o-- " + fieldType.getSimpleName() + " : " + field.getName());
                } else if (fieldType.isArray() && classesToDiagram.contains(fieldType.getComponentType()) && isRootPackage(fieldType.getComponentType())) {
                    relationships.add(clazz.getSimpleName() + " o-- \"0..*\" " + fieldType.getComponentType().getSimpleName() + " : " + field.getName());
                } else if (Collection.class.isAssignableFrom(fieldType) && field.getGenericType() instanceof ParameterizedType) {
                    for (Type t : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
                        if (t instanceof Class && classesToDiagram.contains(t) && isRootPackage((Class<?>) t)) {
                            relationships.add(clazz.getSimpleName() + " o-- \"0..*\" " + ((Class<?>) t).getSimpleName() + " : " + field.getName());
                        }
                    }
                }
            }
            classInfoMap.put(clazz.getName(), fieldsContent); // Use full name for map
        }

        for (java.util.Map.Entry<String, List<String>> entry : classInfoMap.entrySet()) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(entry.getKey(), false, classLoader);
            } catch (ClassNotFoundException e) {}

            if (clazz == null) continue;

            String simpleName = clazz.getSimpleName();
            if (clazz.isEnum()) {
                plantUmlContent.append("enum ").append(simpleName).append(" {\n");
                for (Object enumConstant : clazz.getEnumConstants()) plantUmlContent.append("  ").append(((Enum<?>) enumConstant).name()).append("\n");
            } else if (clazz.isInterface()) plantUmlContent.append("interface ").append(simpleName).append(" {\n");
            else if (Modifier.isAbstract(clazz.getModifiers())) plantUmlContent.append("abstract class ").append(simpleName).append(" {\n");
            else plantUmlContent.append("class ").append(simpleName).append(" {\n");

            for (String field : entry.getValue()) plantUmlContent.append("  ").append(field).append("\n");
            plantUmlContent.append("}\n\n");
        }

        for (Class<?> clazz : classesToDiagram) {
            if (clazz.getSuperclass() != null && classesToDiagram.contains(clazz.getSuperclass())) {
                plantUmlContent.append(clazz.getSuperclass().getSimpleName()).append(" <|-- ").append(clazz.getSimpleName()).append("\n");
            }
        }
        for (String rel : relationships) plantUmlContent.append(rel).append("\n");
        plantUmlContent.append("@enduml\n");

        var outputFile = new File("../site/docs/assets/kurzweil-model.puml");
        try (java.io.FileWriter writer = new java.io.FileWriter(outputFile)) {
            writer.write(plantUmlContent.toString());
            System.out.println("Generated UML diagram source: " + outputFile.getAbsolutePath());
            System.out.println("Please run PlantUML on this file to generate the SVG.");
        } catch (java.io.IOException e) {
            System.err.println("Error writing UML diagram: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
/exit exitCode
