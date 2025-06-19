#!/bin/bash

# Cross-platform Maven dependency updater
# Usage: ./update-dependencies.sh [path/to/pom.xml]

POM_PATH=${1:-"pom.xml"}

# Detect OS and set appropriate temp directory
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    # Windows (Git Bash/Cygwin)
    TEMP_DIR=$(mktemp -d -p "$TEMP")
    CLASSPATH_SEP=";"
else
    # Unix/Linux/Mac
    TEMP_DIR=$(mktemp -d)
    CLASSPATH_SEP=":"
fi

echo "📦 Setting up dependency updater..."
echo "📂 Working directory: $TEMP_DIR"

# Create the Java file
cat > "$TEMP_DIR/MavenDependencyUpdater.java" << 'EOF'
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MavenDependencyUpdater {
    public static void main(String[] args) throws Exception {
        String pomPath = args.length > 0 ? args[0] : "pom.xml";
        System.out.println("🔍 Analyzing: " + pomPath);
        updateDependencies(pomPath);
    }
    
    public static void updateDependencies(String pomPath) throws Exception {
        File pomFile = new File(pomPath);
        if (!pomFile.exists()) {
            System.err.println("❌ Error: pom.xml not found at: " + pomFile.getAbsolutePath());
            return;
        }
        
        // Backup original
        File backupFile = new File(pomPath + ".backup");
        copyFile(pomFile, backupFile);
        System.out.println("💾 Created backup: " + backupFile.getName());
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(pomFile);
        doc.getDocumentElement().normalize();
        
        NodeList dependencies = doc.getElementsByTagName("dependency");
        int updatedCount = 0;
        
        System.out.println("📊 Found " + dependencies.getLength() + " dependencies\n");
        
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node dependency = dependencies.item(i);
            if (dependency.getNodeType() == Node.ELEMENT_NODE) {
                Element depElement = (Element) dependency;
                String groupId = getTextContent(depElement, "groupId");
                String artifactId = getTextContent(depElement, "artifactId");
                String currentVersion = getTextContent(depElement, "version");
                
                if (currentVersion != null && !currentVersion.startsWith("${") && !currentVersion.isEmpty()) {
                    System.out.print("📦 " + groupId + ":" + artifactId + " (" + currentVersion + ")");
                    
                    try {
                        String latestVersion = getLatestVersion(groupId, artifactId);
                        
                        if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                            updateVersion(depElement, latestVersion);
                            System.out.println(" → ✨ " + latestVersion);
                            updatedCount++;
                        } else if (latestVersion != null) {
                            System.out.println(" → ✓ Latest");
                        } else {
                            System.out.println(" → ⚠️  Could not check");
                        }
                        
                        Thread.sleep(500); // Be nice to mvnrepository.com
                    } catch (Exception e) {
                        System.out.println(" → ❌ Error: " + e.getMessage());
                    }
                }
            }
        }
        
        if (updatedCount > 0) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(pomFile);
            transformer.transform(source, result);
            
            System.out.println("\n✅ Updated " + updatedCount + " dependencies!");
        } else {
            System.out.println("\n✅ All dependencies are already up to date!");
        }
    }
    
    private static String getLatestVersion(String groupId, String artifactId) {
        try {
            String url = "https://mvnrepository.com/artifact/" + 
                        URLEncoder.encode(groupId, "UTF-8") + "/" + 
                        URLEncoder.encode(artifactId, "UTF-8");
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            
            // Try multiple selectors
            Element versionLink = doc.select("a.vbtn.release").first();
            if (versionLink == null) {
                versionLink = doc.select("div.version-item a.vnum").first();
            }
            
            return versionLink != null ? versionLink.text().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String getTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0).getTextContent().trim() : null;
    }
    
    private static void updateVersion(Element dependency, String newVersion) {
        NodeList versionNodes = dependency.getElementsByTagName("version");
        if (versionNodes.getLength() > 0) {
            versionNodes.item(0).setTextContent(newVersion);
        }
    }
    
    private static void copyFile(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}
EOF

# Verify Java file was created
if [ ! -f "$TEMP_DIR/MavenDependencyUpdater.java" ]; then
    echo "❌ Error: Failed to create Java file"
    exit 1
fi

# Download JSoup if not present
if [ ! -f "$TEMP_DIR/jsoup.jar" ]; then
    echo "📥 Downloading JSoup..."
    curl -sL "https://repo1.maven.org/maven2/org/jsoup/jsoup/1.17.2/jsoup-1.17.2.jar" -o "$TEMP_DIR/jsoup.jar"
    
    if [ ! -f "$TEMP_DIR/jsoup.jar" ]; then
        echo "❌ Error: Failed to download JSoup"
        exit 1
    fi
fi

# Compile
echo "🔨 Compiling..."
cd "$TEMP_DIR"
javac -cp "jsoup.jar" MavenDependencyUpdater.java

if [ ! -f "MavenDependencyUpdater.class" ]; then
    echo "❌ Error: Compilation failed"
    exit 1
fi

# Go back to original directory
cd - > /dev/null

# Run with absolute path to POM
echo "🚀 Updating dependencies..."
java -cp "$TEMP_DIR${CLASSPATH_SEP}$TEMP_DIR/jsoup.jar" MavenDependencyUpdater "$(realpath "$POM_PATH")"

# Cleanup
rm -rf "$TEMP_DIR"
