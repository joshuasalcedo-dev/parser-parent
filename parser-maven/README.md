

# PARSE MAVEN DEPENDENCIES
# CHECK FOR VULNERABILITY
# LIST AVAILABLE GOALS
# METADATA OF THE PROJECT


https://search.maven.org/solrsearch/select?q=springboot&rows=20&wt=xml	Mimics typing "guice" in the basic search box. Returns first page of artifacts with "guice" in the groupId or artifactId and lists details for most recent version released.
https://search.maven.org/solrsearch/select?q=g:com.google.inject+AND+a:guice&core=gav&rows=20&wt=xml	Mimics clicking the link for all versions of groupId "com.google.inject" and artifactId "guice." Returns sorted list of all versions of an artifact.
https://search.maven.org/solrsearch/select?q=g:com.google.inject&rows=20&wt=json	Search for all artifacts in the groupId "com.google.inject." For each artifact, returns details for the most recent version released.
https://search.maven.org/solrsearch/select?q=a:guice&rows=20&wt=json	Search for any artifactId named "guice," irrespective of groupId. For each artifact returns details for the most recent version released.
https://search.maven.org/remotecontent?filepath=com/jolira/guice/3.0.0/guice-3.0.0.pom	Downloads a file at the given path from the Central Repository (https://repo1.maven.org/maven2/ and its mirrors).
https://search.maven.org/solrsearch/select? q=g:com.google.inject%20AND%20a:guice%20AND%20v:3.0%20AND%20l:javadoc%20AND%20p:jar&rows=20&wt=json	Mimics searching by coordinate in Advanced Search. This search uses all coordinates ("g" for groupId, "a" for artifactId, "v" for version, "p" for packaging, "l" for classifier)
https://search.maven.org/solrsearch/select?q=c:junit&rows=20&wt=json	Mimics searching by classname in Advanced Search. Returns a list of artifacts, down to the specific version, containing the class.
https://search.maven.org/solrsearch/select?q=fc:org.specs.runner.JUnit&rows=20&wt=json	Mimics searching by fully-qualified classname in Advanced Search. Returns a list of artifacts, down to the specific version containing the class.
https://search.maven.org/solrsearch/select?q=1:35379fb6526fd01 9f331542b4e9ae2e566c57933&rows=20&wt=json	Mimics searching by SHA-1 Checksum in Advanced Search. You will need to calculate the SHA-1 for the file before sending the request to the Central Repository.
https://search.maven.org/solrsearch/select?q=tags:sbtplugin&rows=20&wt=json	Mimics searching for tags:sbtplugin in the basic searchbar.
https://search.maven.org/solrsearch/select?q=tags:sbtVersion-0.11&rows=20&wt=json	Mimics searching for tags:sbtVersion-0.11 in the basic searchbar.
https://search.maven.org/solrsearch/select?q=tags:scalaVersion-2.9&rows=20&wt=json	Mimics searching for tags:scalaVersion-2.9 in the basic search bar.


You can programmatically get the Maven dependency tree using several approaches:

## 1. Using Maven Invoker API

```java
import org.apache.maven.shared.invoker.*;
import java.io.File;
import java.util.Collections;

public class MavenDependencyTree {
    public static void main(String[] args) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setGoals(Collections.singletonList("dependency:tree"));
        
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("/path/to/maven"));
        
        InvocationResult result = invoker.execute(request);
        
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }
    }
}
```

## 2. Using Maven Resolver (Aether)

```java
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class DependencyTreeResolver {
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    
    public DependencyNode resolveDependencyTree(String groupId, String artifactId, String version) 
            throws Exception {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        
        DependencyNode node = system.collectDependencies(session, collectRequest).getRoot();
        
        DependencyRequest depRequest = new DependencyRequest();
        depRequest.setRoot(node);
        
        system.resolveDependencies(session, depRequest);
        
        return node;
    }
    
    public void printDependencyTree(DependencyNode node) {
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        System.out.println(nlg.getClassPath());
    }
}
```

## 3. Using Maven Embedder

```java
import org.apache.maven.cli.MavenCli;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class MavenEmbedderExample {
    public static String getDependencyTree(String projectPath) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        
        MavenCli cli = new MavenCli();
        cli.doMain(new String[]{"dependency:tree"}, projectPath, ps, ps);
        
        return baos.toString();
    }
}
```

## 4. Using Maven Model Builder

```java
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.resolution.ModelResolver;

public class ModelDependencyReader {
    public Model readModel(File pomFile) throws Exception {
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(pomFile);
        request.setModelResolver(new YourModelResolver()); // Custom resolver
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        
        DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory();
        ModelBuilder builder = factory.newInstance();
        ModelBuildingResult result = builder.build(request);
        
        return result.getEffectiveModel();
    }
}
```

## 5. Using Maven Plugin API (for plugin development)

```java
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;

@Mojo(name = "dependency-tree")
public class DependencyTreeMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    public void execute() {
        printDependencies(project.getArtifacts(), 0);
    }
    
    private void printDependencies(Set<Artifact> artifacts, int depth) {
        for (Artifact artifact : artifacts) {
            getLog().info(String.format("%s%s:%s:%s",
                "  ".repeat(depth),
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion()));
        }
    }
}
```

## Required Dependencies

Add these to your `pom.xml`:

```xml
<!-- For Maven Invoker -->
<dependency>
    <groupId>org.apache.maven.shared</groupId>
    <artifactId>maven-invoker</artifactId>
    <version>3.2.0</version>
</dependency>

<!-- For Maven Resolver (Aether) -->
<dependency>
    <groupId>org.apache.maven.resolver</groupId>
    <artifactId>maven-resolver-api</artifactId>
    <version>1.9.18</version>
</dependency>
<dependency>
    <groupId>org.apache.maven.resolver</groupId>
    <artifactId>maven-resolver-impl</artifactId>
    <version>1.9.18</version>
</dependency>

<!-- For Maven Embedder -->
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-embedder</artifactId>
    <version>3.9.6</version>
</dependency>

<!-- For Model Building -->
<dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-model-builder</artifactId>
    <version>3.9.6</version>
</dependency>
```

The Maven Resolver approach is generally recommended for complex dependency resolution tasks, while the Maven Invoker is simpler for basic needs. Choose based on your specific requirements for parsing and processing the dependency tree.