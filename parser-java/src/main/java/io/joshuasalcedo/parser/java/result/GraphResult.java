package io.joshuasalcedo.parser.java.result;

import java.util.Map;

/**
 * Result of graph analysis containing multiple graph format representations
 */
public record GraphResult(
    String dotGraph,         // Graphviz DOT format
    String mermaidDiagram,   // Mermaid diagram
    String graphML,          // GraphML format
    String plantUML,         // Enhanced PlantUML
    String d2Diagram,        // D2 diagram format
    Map<String, Object> graphMetrics
) {
    
    public boolean hasCircularDependencies() {
        return graphMetrics.containsKey("circularDependencies") && 
               (int) graphMetrics.get("circularDependencies") > 0;
    }
    
    public double getGraphDensity() {
        return (double) graphMetrics.getOrDefault("density", 0.0);
    }
    
    public int getNodeCount() {
        return (int) graphMetrics.getOrDefault("nodeCount", 0);
    }
    
    public int getEdgeCount() {
        return (int) graphMetrics.getOrDefault("edgeCount", 0);
    }
    
    /**
     * Generate an HTML page with interactive diagrams
     */
    public String generateInteractiveHTML() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<title>Project Architecture Visualization</title>\n");
        html.append("<script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>\n");
        html.append("<script>mermaid.initialize({ startOnLoad: true });</script>\n");
        html.append("<style>\n");
        html.append("  body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("  .tab { overflow: hidden; border: 1px solid #ccc; background-color: #f1f1f1; }\n");
        html.append("  .tab button { background-color: inherit; float: left; border: none; outline: none;\n");
        html.append("    cursor: pointer; padding: 14px 16px; transition: 0.3s; }\n");
        html.append("  .tab button:hover { background-color: #ddd; }\n");
        html.append("  .tab button.active { background-color: #ccc; }\n");
        html.append("  .tabcontent { display: none; padding: 20px; border: 1px solid #ccc; border-top: none; }\n");
        html.append("  .metrics { background-color: #f0f0f0; padding: 10px; margin: 10px 0; border-radius: 5px; }\n");
        html.append("  pre { background-color: #f5f5f5; padding: 10px; overflow-x: auto; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        html.append("<h1>Project Architecture Visualization</h1>\n");
        
        // Metrics section
        html.append("<div class=\"metrics\">\n");
        html.append("<h2>Graph Metrics</h2>\n");
        html.append("<p><strong>Nodes:</strong> ").append(getNodeCount()).append("</p>\n");
        html.append("<p><strong>Edges:</strong> ").append(getEdgeCount()).append("</p>\n");
        html.append("<p><strong>Density:</strong> ").append(String.format("%.3f", getGraphDensity())).append("</p>\n");
        if (hasCircularDependencies()) {
            html.append("<p style=\"color: red;\"><strong>⚠ Circular Dependencies Detected!</strong></p>\n");
        }
        html.append("</div>\n");
        
        // Tabs
        html.append("<div class=\"tab\">\n");
        html.append("  <button class=\"tablinks active\" onclick=\"openTab(event, 'Mermaid')\">Mermaid</button>\n");
        html.append("  <button class=\"tablinks\" onclick=\"openTab(event, 'DOT')\">Graphviz DOT</button>\n");
        html.append("  <button class=\"tablinks\" onclick=\"openTab(event, 'PlantUML')\">PlantUML</button>\n");
        html.append("  <button class=\"tablinks\" onclick=\"openTab(event, 'D2')\">D2</button>\n");
        html.append("  <button class=\"tablinks\" onclick=\"openTab(event, 'GraphML')\">GraphML</button>\n");
        html.append("</div>\n");
        
        // Tab contents
        html.append("<div id=\"Mermaid\" class=\"tabcontent\" style=\"display:block;\">\n");
        html.append("  <h3>Interactive Mermaid Diagram</h3>\n");
        html.append("  <div class=\"mermaid\">\n").append(mermaidDiagram).append("  </div>\n");
        html.append("</div>\n");
        
        html.append("<div id=\"DOT\" class=\"tabcontent\">\n");
        html.append("  <h3>Graphviz DOT</h3>\n");
        html.append("  <p>Copy this to <a href=\"https://dreampuf.github.io/GraphvizOnline/\" target=\"_blank\">GraphvizOnline</a></p>\n");
        html.append("  <pre>").append(escapeHtml(dotGraph)).append("</pre>\n");
        html.append("</div>\n");
        
        html.append("<div id=\"PlantUML\" class=\"tabcontent\">\n");
        html.append("  <h3>PlantUML</h3>\n");
        html.append("  <p>Copy this to <a href=\"http://www.plantuml.com/plantuml/uml/\" target=\"_blank\">PlantUML Web Server</a></p>\n");
        html.append("  <pre>").append(escapeHtml(plantUML)).append("</pre>\n");
        html.append("</div>\n");
        
        html.append("<div id=\"D2\" class=\"tabcontent\">\n");
        html.append("  <h3>D2 Diagram</h3>\n");
        html.append("  <p>Copy this to <a href=\"https://play.d2lang.com/\" target=\"_blank\">D2 Playground</a></p>\n");
        html.append("  <pre>").append(escapeHtml(d2Diagram)).append("</pre>\n");
        html.append("</div>\n");
        
        html.append("<div id=\"GraphML\" class=\"tabcontent\">\n");
        html.append("  <h3>GraphML</h3>\n");
        html.append("  <p>Download and open with <a href=\"https://www.yworks.com/products/yed\" target=\"_blank\">yEd</a> or Gephi</p>\n");
        html.append("  <pre>").append(escapeHtml(graphML)).append("</pre>\n");
        html.append("</div>\n");
        
        // JavaScript for tabs
        html.append("<script>\n");
        html.append("function openTab(evt, tabName) {\n");
        html.append("  var i, tabcontent, tablinks;\n");
        html.append("  tabcontent = document.getElementsByClassName(\"tabcontent\");\n");
        html.append("  for (i = 0; i < tabcontent.length; i++) {\n");
        html.append("    tabcontent[i].style.display = \"none\";\n");
        html.append("  }\n");
        html.append("  tablinks = document.getElementsByClassName(\"tablinks\");\n");
        html.append("  for (i = 0; i < tablinks.length; i++) {\n");
        html.append("    tablinks[i].className = tablinks[i].className.replace(\" active\", \"\");\n");
        html.append("  }\n");
        html.append("  document.getElementById(tabName).style.display = \"block\";\n");
        html.append("  evt.currentTarget.className += \" active\";\n");
        html.append("}\n");
        html.append("</script>\n");
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}