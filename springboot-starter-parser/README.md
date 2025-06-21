# Spring Boot Starter - Java Parser

A powerful Spring Boot starter that automatically analyzes Java projects and provides comprehensive insights through a beautiful web dashboard and REST API.

## 🚀 Features

- **Zero Configuration** - Works out of the box with no setup required
- **Automatic Analysis** - Analyzes your project on application startup
- **Beautiful Web Dashboard** - Interactive charts, graphs, and metrics
- **REST API** - Full REST endpoints for programmatic access
- **Dependency Graph Visualization** - Interactive D3.js dependency graphs
- **Code Quality Metrics** - Complexity analysis, health scoring, and recommendations
- **OpenAPI Documentation** - Automatic API documentation with Swagger UI
- **Circular Dependency Detection** - Identifies and warns about circular dependencies
- **Project Health Assessment** - Risk analysis and improvement recommendations

## 📦 Installation

Add the dependency to your Spring Boot project:

### Maven

```xml
<dependency>
    <groupId>io.joshuasalcedo.parser</groupId>
    <artifactId>springboot-starter-parser</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.joshuasalcedo.parser:springboot-starter-parser:1.0-SNAPSHOT'
```

## 🎯 Quick Start

1. **Add the dependency** to your Spring Boot project
2. **Start your application** - No configuration needed!
3. **Open your browser** and visit: `http://localhost:8080/api/parser/view`

That's it! The starter will automatically analyze your project and provide a comprehensive dashboard.

## 🌐 Endpoints

Once your application starts, the following endpoints are available:

### Web Dashboard
- **Main Dashboard**: `http://localhost:8080/api/parser/view`
- **Dependency Graph**: `http://localhost:8080/api/parser/view/dependencies`
- **Metrics View**: `http://localhost:8080/api/parser/view/metrics`

### REST API
- **Project Analysis**: `GET /api/parser/analysis`
- **Summary**: `GET /api/parser/summary`
- **Dependencies**: `GET /api/parser/dependencies`
- **Metrics**: `GET /api/parser/metrics`
- **Health Assessment**: `GET /api/parser/health`
- **Code Patterns**: `GET /api/parser/patterns`
- **Analysis Status**: `GET /api/parser/status`
- **Clear Cache**: `DELETE /api/parser/cache`

### Documentation
- **OpenAPI Docs**: `http://localhost:8080/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8080/v3/api-docs`

## ⚙️ Configuration

The starter works with **zero configuration**, but you can customize it if needed.

### Default Configuration

The starter uses these default values:

```properties
# Auto-analysis on startup (default: true)
parser.auto-analyze-on-startup=true

# Project path to analyze (default: current directory)
parser.project-path=.

# Enable parallel analysis for better performance (default: true)
parser.parallel-analysis=true

# Thread pool size for parallel analysis (default: 4)
parser.thread-pool-size=4

# Analysis features (all default: true)
parser.analyze-dependencies=true
parser.analyze-metrics=true
parser.generate-graphs=true

# Web interface (default: true)
parser.enable-web-view=true

# API and view paths
parser.api-path=/api/parser
parser.view-path=/api/parser/view
```

### Custom Configuration Template

Create an `application.properties` or `application.yml` file to customize:

#### application.properties
```properties
# === Java Parser Configuration ===

# Auto-analyze project on application startup
parser.auto-analyze-on-startup=true

# Path to the project you want to analyze
# Leave empty or set to "." for current directory
parser.project-path=.

# Performance Settings
parser.parallel-analysis=true
parser.thread-pool-size=8

# Analysis Features
parser.analyze-dependencies=true
parser.analyze-metrics=true
parser.generate-graphs=true

# Web Interface
parser.enable-web-view=true

# Custom Paths (optional)
parser.api-path=/api/parser
parser.view-path=/api/parser/view

# === Spring Boot Settings ===

# Server port (optional)
server.port=8080

# Application name
spring.application.name=my-java-project-analyzer

# Logging level for parser (optional)
logging.level.io.joshuasalcedo.parser=INFO
```

#### application.yml
```yml
parser:
  auto-analyze-on-startup: true
  project-path: "."
  parallel-analysis: true
  thread-pool-size: 8
  analyze-dependencies: true
  analyze-metrics: true
  generate-graphs: true
  enable-web-view: true
  api-path: "/api/parser"
  view-path: "/api/parser/view"

server:
  port: 8080

spring:
  application:
    name: "my-java-project-analyzer"

logging:
  level:
    io.joshuasalcedo.parser: INFO
```

## 📊 Dashboard Features

### Main Dashboard
- **Project Overview** - Classes, methods, interfaces, lines of code
- **Health Score** - Overall project health rating
- **Complexity Charts** - Visual complexity distribution
- **Quality Metrics** - Code quality radar chart
- **Mini Dependency Graph** - Quick overview of dependencies

### Dependency Graph View
- **Interactive Graph** - D3.js powered dependency visualization
- **Node Details** - Click nodes to see detailed information
- **Graph Controls** - Zoom, pan, center, pause physics
- **Circular Dependency Warnings** - Automatic detection and highlighting
- **Legend and Statistics** - Understanding the graph

### Metrics View
- **Detailed Metrics** - Comprehensive code metrics
- **Complexity Analysis** - Method and class complexity
- **Quality Scores** - Various quality indicators
- **Recommendations** - Actionable improvement suggestions

## 🔧 Advanced Usage

### Custom Project Path

To analyze a different project directory:

```properties
parser.project-path=/path/to/your/java/project
```

### Performance Tuning

For large projects, optimize performance:

```properties
# Increase thread pool for faster analysis
parser.thread-pool-size=16

# Enable parallel analysis
parser.parallel-analysis=true
```

### Disable Features

To disable specific features:

```properties
# Disable web dashboard
parser.enable-web-view=false

# Disable automatic startup analysis
parser.auto-analyze-on-startup=false

# Disable specific analysis types
parser.analyze-dependencies=false
parser.analyze-metrics=false
parser.generate-graphs=false
```

### Custom Paths

Change the API and view paths:

```properties
# Custom API path
parser.api-path=/custom/api/path

# Custom view path  
parser.view-path=/custom/dashboard
```

## 🎨 Web Dashboard

The web dashboard provides a beautiful, responsive interface with:

- **Modern UI** - Clean, professional design with Bootstrap 5
- **Interactive Charts** - Chart.js powered visualizations
- **Dependency Graphs** - D3.js interactive network graphs
- **Responsive Design** - Works on desktop, tablet, and mobile
- **Real-time Updates** - Live analysis status and results
- **Export Options** - Download charts and data
- **Keyboard Shortcuts** - Quick navigation and actions

## 🔍 API Usage Examples

### Get Project Analysis
```bash
curl -X GET http://localhost:8080/api/parser/analysis
```

### Get Project Summary
```bash
curl -X GET http://localhost:8080/api/parser/summary
```

### Get Dependencies
```bash
curl -X GET http://localhost:8080/api/parser/dependencies
```

### Clear Analysis Cache
```bash
curl -X DELETE http://localhost:8080/api/parser/cache
```

## 🔧 Troubleshooting

### Common Issues

1. **No Analysis Data**
   - Check that `parser.auto-analyze-on-startup=true`
   - Verify the `parser.project-path` points to a valid Java project
   - Check logs for analysis errors

2. **Dashboard Not Loading**
   - Ensure `parser.enable-web-view=true`
   - Check that port 8080 is not blocked
   - Verify static resources are accessible

3. **Performance Issues**
   - Increase `parser.thread-pool-size` for large projects
   - Enable `parser.parallel-analysis=true`
   - Consider disabling heavy features temporarily

4. **Memory Issues**
   - Increase JVM heap size: `-Xmx2g`
   - Reduce thread pool size if needed
   - Analyze smaller project sections

### Debug Logging

Enable debug logging to troubleshoot:

```properties
logging.level.io.joshuasalcedo.parser=DEBUG
logging.level.io.joshuasalcedo.parser.spring=TRACE
```

## 📝 Examples

### Basic Spring Boot Application

```java
@SpringBootApplication
public class MyAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAnalyzerApplication.class, args);
        
        // Parser automatically starts analyzing your project!
        // Visit http://localhost:8080/api/parser/view
    }
}
```

### Custom Configuration Bean

```java
@Configuration
public class ParserConfig {
    
    @Bean
    @Primary
    public ParserProperties customParserProperties() {
        ParserProperties props = new ParserProperties();
        props.setProjectPath("/custom/project/path");
        props.setThreadPoolSize(8);
        props.setParallelAnalysis(true);
        return props;
    }
}
```

### Using the API Programmatically

```java
@RestController
public class MyController {
    
    @Autowired
    private ParserController parserController;
    
    @GetMapping("/my-analysis")
    public ResponseEntity<ProjectAnalysisResult> getAnalysis() {
        return parserController.getAnalysis();
    }
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- [JavaParser](https://github.com/javaparser/javaparser) - For AST parsing
- [Spring Boot](https://spring.io/projects/spring-boot) - For the framework
- [D3.js](https://d3js.org/) - For graph visualizations  
- [Chart.js](https://www.chartjs.org/) - For charts
- [Bootstrap](https://getbootstrap.com/) - For UI components

---

**Happy Analyzing! 🎉**

For issues, feature requests, or questions, please open an issue on GitHub.