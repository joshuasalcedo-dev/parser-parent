<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Java Project Analysis Report - ${projectName}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f5f5f5;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
        }
        
        .header .subtitle {
            font-size: 1.2em;
            opacity: 0.9;
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 30px 0;
        }
        
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            text-align: center;
            border-left: 4px solid #667eea;
        }
        
        .stat-card h3 {
            font-size: 2em;
            color: #667eea;
            margin-bottom: 5px;
        }
        
        .stat-card p {
            color: #666;
            font-weight: 500;
        }
        
        .section {
            background: white;
            margin: 20px 0;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        
        .section-header {
            background: #667eea;
            color: white;
            padding: 20px;
            font-size: 1.5em;
            font-weight: bold;
        }
        
        .section-content {
            padding: 20px;
        }
        
        .table-container {
            overflow-x: auto;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        
        th {
            background-color: #f8f9fa;
            font-weight: bold;
            color: #333;
        }
        
        tr:hover {
            background-color: #f5f5f5;
        }
        
        .complexity-high { color: #dc3545; font-weight: bold; }
        .complexity-medium { color: #fd7e14; font-weight: bold; }
        .complexity-low { color: #28a745; font-weight: bold; }
        
        .module-section {
            border-left: 4px solid #28a745;
            margin: 20px 0;
        }
        
        .module-header {
            background: #28a745;
        }
        
        .progress-bar {
            width: 100%;
            height: 20px;
            background-color: #e9ecef;
            border-radius: 10px;
            overflow: hidden;
            margin: 10px 0;
        }
        
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #28a745, #20c997);
            transition: width 0.3s ease;
        }
        
        .chart-container {
            height: 300px;
            margin: 20px 0;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #f8f9fa;
            border-radius: 8px;
        }
        
        .generated-info {
            margin-top: 40px;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            color: #666;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 ${projectName}</h1>
            <div class="subtitle">Comprehensive Java Project Analysis Report</div>
            <div class="subtitle">Generated on ${timestamp}</div>
        </div>

        <!-- Project Statistics -->
        <div class="stats-grid">
            <div class="stat-card">
                <h3>${statistics.totalClasses}</h3>
                <p>Classes</p>
            </div>
            <div class="stat-card">
                <h3>${statistics.totalMethods}</h3>
                <p>Methods</p>
            </div>
            <div class="stat-card">
                <h3>${statistics.totalFields}</h3>
                <p>Fields</p>
            </div>
            <div class="stat-card">
                <h3>${statistics.totalLinesOfCode}</h3>
                <p>Lines of Code</p>
            </div>
            <div class="stat-card">
                <h3>${statistics.totalInterfaces}</h3>
                <p>Interfaces</p>
            </div>
            <div class="stat-card">
                <h3>${statistics.totalEnums}</h3>
                <p>Enums</p>
            </div>
        </div>

        <!-- Class Complexity Analysis -->
        <div class="section">
            <div class="section-header">🔍 Class Complexity Analysis</div>
            <div class="section-content">
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Class Name</th>
                                <th>Methods</th>
                                <th>Fields</th>
                                <th>Complexity</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list classComplexities as complexity>
                            <tr>
                                <td>${complexity.className}</td>
                                <td>${complexity.methodCount}</td>
                                <td>${complexity.fieldCount}</td>
                                <td>
                                    <#if (complexity.dependencyCount > 10)>
                                        <span class="complexity-high">${complexity.dependencyCount}</span>
                                    <#elseif (complexity.dependencyCount > 5)>
                                        <span class="complexity-medium">${complexity.dependencyCount}</span>
                                    <#else>
                                        <span class="complexity-low">${complexity.dependencyCount}</span>
                                    </#if>
                                </td>
                                <td>
                                    <#if (complexity.dependencyCount > 10)>
                                        <span class="complexity-high">High Complexity</span>
                                    <#elseif (complexity.dependencyCount > 5)>
                                        <span class="complexity-medium">Medium Complexity</span>
                                    <#else>
                                        <span class="complexity-low">Low Complexity</span>
                                    </#if>
                                </td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Package Distribution -->
        <div class="section">
            <div class="section-header">📦 Package Distribution</div>
            <div class="section-content">
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Package</th>
                                <th>Class Count</th>
                                <th>Percentage</th>
                                <th>Distribution</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list packageDistribution?keys as packageName>
                            <tr>
                                <td>${packageName}</td>
                                <td>${packageDistribution[packageName]}</td>
                                <td>${((packageDistribution[packageName] / statistics.totalClasses) * 100)?string("0.0")}%</td>
                                <td>
                                    <div class="progress-bar">
                                        <div class="progress-fill" style="width: ${((packageDistribution[packageName] / statistics.totalClasses) * 100)?string("0.0")}%"></div>
                                    </div>
                                </td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Submodules Section -->
        <#if submodules?has_content>
        <div class="section">
            <div class="section-header">🏗️ Submodules Analysis</div>
            <div class="section-content">
                <#list submodules as submodule>
                <div class="module-section section">
                    <div class="module-header section-header">
                        📂 ${submodule.name}
                    </div>
                    <div class="section-content">
                        <div class="stats-grid">
                            <div class="stat-card">
                                <h3>${submodule.statistics.totalClasses}</h3>
                                <p>Classes</p>
                            </div>
                            <div class="stat-card">
                                <h3>${submodule.statistics.totalMethods}</h3>
                                <p>Methods</p>
                            </div>
                            <div class="stat-card">
                                <h3>${submodule.statistics.totalFields}</h3>
                                <p>Fields</p>
                            </div>
                            <div class="stat-card">
                                <h3>${submodule.statistics.totalLinesOfCode}</h3>
                                <p>Lines of Code</p>
                            </div>
                        </div>
                        
                        <h4>Top Classes by Complexity</h4>
                        <div class="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Class Name</th>
                                        <th>Methods</th>
                                        <th>Fields</th>
                                        <th>Dependencies</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <#list submodule.topComplexClasses as complexity>
                                    <tr>
                                        <td>${complexity.className}</td>
                                        <td>${complexity.methodCount}</td>
                                        <td>${complexity.fieldCount}</td>
                                        <td>${complexity.dependencyCount}</td>
                                    </tr>
                                    </#list>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
                </#list>
            </div>
        </div>
        </#if>

        <!-- Method Usage Statistics -->
        <#if methodUsage?has_content>
        <div class="section">
            <div class="section-header">⚡ Method Usage Analysis</div>
            <div class="section-content">
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Method</th>
                                <th>Class</th>
                                <th>Usage Count</th>
                                <th>Visibility</th>
                            </tr>
                        </thead>
                        <tbody>
                            <#list methodUsage as usage>
                            <tr>
                                <td>${usage.methodName}</td>
                                <td>${usage.className}</td>
                                <td>${usage.usageCount}</td>
                                <td>${usage.visibility}</td>
                            </tr>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        </#if>

        <div class="generated-info">
            <p>🤖 Generated with Claude Code Analysis Tool</p>
            <p>Analysis completed in ${analysisTime!"N/A"}ms</p>
        </div>
    </div>
</body>
</html>