/**
 * Java Parser Dashboard JavaScript
 * Interactive functionality for the parser dashboard and graph visualizations
 */

class ParserDashboard {
    constructor() {
        this.charts = {};
        this.simulation = null;
        this.tooltip = null;
        this.selectedNode = null;
        this.init();
    }

    init() {
        this.initTooltips();
        this.initEventListeners();
        this.initCharts();
        this.initGraphs();
        console.log('Parser Dashboard initialized');
    }

    initTooltips() {
        // Initialize Bootstrap tooltips if available
        if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        }
    }

    initEventListeners() {
        // Sidebar toggle for mobile
        const sidebarToggle = document.getElementById('sidebarToggle');
        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', this.toggleSidebar);
        }

        // Refresh data button
        const refreshBtn = document.getElementById('refreshData');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', this.refreshDashboard.bind(this));
        }

        // Export functionality
        const exportBtn = document.getElementById('exportData');
        if (exportBtn) {
            exportBtn.addEventListener('click', this.exportData.bind(this));
        }

        // Theme toggle
        const themeToggle = document.getElementById('themeToggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', this.toggleTheme.bind(this));
        }

        // Keyboard shortcuts
        document.addEventListener('keydown', this.handleKeyboardShortcuts.bind(this));

        // Window resize handler
        window.addEventListener('resize', this.handleResize.bind(this));
    }

    initCharts() {
        this.initComplexityChart();
        this.initQualityChart();
        this.initDuplicationChart();
        this.initTrendChart();
    }

    initComplexityChart() {
        const ctx = document.getElementById('complexityChart');
        if (!ctx || typeof Chart === 'undefined') return;

        const data = window.complexityChartData || this.generateMockComplexityData();
        
        this.charts.complexity = new Chart(ctx, {
            type: 'bar',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            title: (items) => `Class: ${items[0].label}`,
                            label: (item) => `Complexity: ${item.raw}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(0,0,0,0.1)' },
                        ticks: { color: '#666' }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { 
                            color: '#666',
                            maxRotation: 45
                        }
                    }
                },
                onHover: (event, elements) => {
                    event.native.target.style.cursor = elements.length ? 'pointer' : 'default';
                },
                onClick: (event, elements) => {
                    if (elements.length) {
                        const index = elements[0].index;
                        this.showClassDetails(data.labels[index]);
                    }
                }
            }
        });
    }

    initQualityChart() {
        const ctx = document.getElementById('qualityChart');
        if (!ctx || typeof Chart === 'undefined') return;

        const data = window.qualityChartData || this.generateMockQualityData();

        this.charts.quality = new Chart(ctx, {
            type: 'radar',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    r: {
                        beginAtZero: true,
                        max: 100,
                        grid: { color: 'rgba(0,0,0,0.1)' },
                        pointLabels: { 
                            color: '#666',
                            font: { size: 12, weight: 'bold' }
                        }
                    }
                }
            }
        });
    }

    initDuplicationChart() {
        const ctx = document.getElementById('duplicationChart');
        if (!ctx || typeof Chart === 'undefined') return;

        const data = window.duplicationChartData || this.generateMockDuplicationData();

        this.charts.duplication = new Chart(ctx, {
            type: 'doughnut',
            data: data,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { 
                            color: '#666',
                            font: { size: 12 }
                        }
                    }
                },
                cutout: '60%'
            }
        });
    }

    initTrendChart() {
        const ctx = document.getElementById('trendChart');
        if (!ctx || typeof Chart === 'undefined') return;

        this.charts.trend = new Chart(ctx, {
            type: 'line',
            data: this.generateMockTrendData(),
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { 
                        display: true,
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(0,0,0,0.1)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        });
    }

    initGraphs() {
        this.initDependencyGraph();
        this.initNetworkGraph();
    }

    initDependencyGraph() {
        const container = document.getElementById('dependencyGraph');
        if (!container || typeof d3 === 'undefined') return;

        const data = window.dependencyGraphData || this.generateMockGraphData();
        this.createInteractiveDependencyGraph(container, data);
    }

    createInteractiveDependencyGraph(container, data) {
        const width = container.clientWidth || 800;
        const height = container.clientHeight || 400;

        // Clear existing content
        d3.select(container).selectAll("*").remove();

        const svg = d3.select(container)
            .append("svg")
            .attr("width", width)
            .attr("height", height)
            .call(d3.zoom()
                .scaleExtent([0.1, 4])
                .on("zoom", (event) => {
                    g.attr("transform", event.transform);
                }))
            .on("dblclick.zoom", null);

        const g = svg.append("g");

        // Create tooltip
        this.tooltip = d3.select("body").append("div")
            .attr("class", "tooltip")
            .style("opacity", 0);

        // Force simulation
        this.simulation = d3.forceSimulation(data.nodes)
            .force("link", d3.forceLink(data.links).id(d => d.id).distance(100))
            .force("charge", d3.forceManyBody().strength(-300))
            .force("center", d3.forceCenter(width / 2, height / 2))
            .force("collision", d3.forceCollide().radius(d => this.getNodeRadius(d) + 5));

        // Create links
        const link = g.append("g")
            .selectAll("line")
            .data(data.links)
            .enter().append("line")
            .attr("class", "link");

        // Create nodes
        const node = g.append("g")
            .selectAll("circle")
            .data(data.nodes)
            .enter().append("circle")
            .attr("class", d => `node ${this.getNodeType(d)}`)
            .attr("r", d => this.getNodeRadius(d))
            .on("click", (event, d) => this.handleNodeClick(event, d, node, link))
            .on("mouseover", (event, d) => this.showTooltip(event, d))
            .on("mouseout", () => this.hideTooltip())
            .call(d3.drag()
                .on("start", (event, d) => this.dragstarted(event, d))
                .on("drag", (event, d) => this.dragged(event, d))
                .on("end", (event, d) => this.dragended(event, d)));

        // Create labels
        const labels = g.append("g")
            .selectAll("text")
            .data(data.nodes)
            .enter().append("text")
            .text(d => d.name)
            .style("font-size", "11px")
            .style("text-anchor", "middle")
            .style("pointer-events", "none")
            .style("fill", "#333");

        // Update positions on tick
        this.simulation.on("tick", () => {
            link
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

            node
                .attr("cx", d => d.x)
                .attr("cy", d => d.y);

            labels
                .attr("x", d => d.x)
                .attr("y", d => d.y + this.getNodeRadius(d) + 15);
        });

        // Store references for controls
        this.graphElements = { svg, g, node, link, labels };
    }

    getNodeType(d) {
        if (d.name && d.name.includes('Interface')) return 'interface';
        if (d.name && d.name.includes('Enum')) return 'enum';
        return 'class';
    }

    getNodeRadius(d) {
        return Math.min(20, 6 + (d.methods || 0) * 1.2);
    }

    handleNodeClick(event, d, nodes, links) {
        // Clear previous selections
        nodes.classed("selected", false);
        links.classed("highlighted", false);

        // Select current node
        d3.select(event.target).classed("selected", true);
        this.selectedNode = d;

        // Highlight connected links
        links.classed("highlighted", l => l.source === d || l.target === d);

        // Show node details
        this.showNodeDetails(d);

        // Prevent event bubbling
        event.stopPropagation();
    }

    showTooltip(event, d) {
        if (!this.tooltip) return;

        this.tooltip.transition().duration(200).style("opacity", .9);
        this.tooltip.html(`
            <strong>${d.name}</strong><br/>
            Package: ${d.package || 'Default'}<br/>
            Methods: ${d.methods || 0}<br/>
            Fields: ${d.fields || 0}
        `)
        .style("left", (event.pageX + 10) + "px")
        .style("top", (event.pageY - 28) + "px");
    }

    hideTooltip() {
        if (this.tooltip) {
            this.tooltip.transition().duration(500).style("opacity", 0);
        }
    }

    showNodeDetails(d) {
        const detailsPanel = document.getElementById('nodeDetails');
        const detailsContent = document.getElementById('nodeInfo');
        
        if (!detailsPanel || !detailsContent) return;

        detailsContent.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6><strong>${d.name}</strong></h6>
                    <p><strong>Package:</strong> ${d.package || 'Default'}</p>
                    <p><strong>Type:</strong> ${this.getNodeType(d)}</p>
                    <p><strong>Methods:</strong> ${d.methods || 0}</p>
                    <p><strong>Fields:</strong> ${d.fields || 0}</p>
                </div>
                <div class="col-md-6">
                    <h6>Dependencies</h6>
                    <div class="mb-3">
                        <span class="badge bg-primary">Class A</span>
                        <span class="badge bg-primary">Class B</span>
                    </div>
                    <h6>Dependents</h6>
                    <div>
                        <span class="badge bg-secondary">Class C</span>
                        <span class="badge bg-secondary">Class D</span>
                    </div>
                </div>
            </div>
        `;

        detailsPanel.style.display = 'block';
        detailsPanel.scrollIntoView({ behavior: 'smooth' });
    }

    // Graph control methods
    resetZoom() {
        if (this.graphElements && this.graphElements.svg) {
            this.graphElements.svg.transition().duration(750).call(
                d3.zoom().transform,
                d3.zoomIdentity
            );
        }
    }

    centerGraph() {
        if (!this.graphElements) return;

        const { svg, g } = this.graphElements;
        const bounds = g.node().getBBox();
        const fullWidth = svg.attr("width");
        const fullHeight = svg.attr("height");
        const centerX = bounds.x + bounds.width / 2;
        const centerY = bounds.y + bounds.height / 2;
        const scale = 0.8 / Math.max(bounds.width / fullWidth, bounds.height / fullHeight);
        const translate = [fullWidth / 2 - scale * centerX, fullHeight / 2 - scale * centerY];

        svg.transition().duration(750).call(
            d3.zoom().transform,
            d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale)
        );
    }

    togglePhysics() {
        const button = document.getElementById('togglePhysics');
        if (!this.simulation || !button) return;

        if (this.simulation.alpha() > 0) {
            this.simulation.stop();
            button.innerHTML = '<i class="fas fa-play"></i> Resume Physics';
        } else {
            this.simulation.restart();
            button.innerHTML = '<i class="fas fa-pause"></i> Pause Physics';
        }
    }

    // Drag handlers
    dragstarted(event, d) {
        if (!event.active) this.simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }

    dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }

    dragended(event, d) {
        if (!event.active) this.simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }

    // Utility methods
    toggleSidebar() {
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.toggle('show');
        }
    }

    refreshDashboard() {
        console.log('Refreshing dashboard...');
        // Show loading state
        this.showLoading();
        
        // Simulate API call
        setTimeout(() => {
            this.hideLoading();
            this.updateCharts();
            this.showNotification('Dashboard refreshed successfully!', 'success');
        }, 2000);
    }

    exportData() {
        console.log('Exporting data...');
        // Create export data
        const exportData = {
            timestamp: new Date().toISOString(),
            projectStats: this.getProjectStats(),
            charts: this.getChartsData()
        };

        // Download as JSON
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `parser-analysis-${new Date().toISOString().split('T')[0]}.json`;
        a.click();
        URL.revokeObjectURL(url);

        this.showNotification('Data exported successfully!', 'success');
    }

    toggleTheme() {
        document.body.classList.toggle('dark-theme');
        const isDark = document.body.classList.contains('dark-theme');
        localStorage.setItem('parser-theme', isDark ? 'dark' : 'light');
        this.showNotification(`Switched to ${isDark ? 'dark' : 'light'} theme`, 'info');
    }

    handleKeyboardShortcuts(event) {
        if (event.ctrlKey || event.metaKey) {
            switch (event.key) {
                case 'r':
                    event.preventDefault();
                    this.refreshDashboard();
                    break;
                case 'e':
                    event.preventDefault();
                    this.exportData();
                    break;
                case 't':
                    event.preventDefault();
                    this.toggleTheme();
                    break;
            }
        }
    }

    handleResize() {
        // Resize charts
        Object.values(this.charts).forEach(chart => {
            if (chart && chart.resize) {
                chart.resize();
            }
        });

        // Resize graphs
        if (this.graphElements) {
            const container = document.getElementById('dependencyGraph');
            if (container) {
                const width = container.clientWidth;
                const height = container.clientHeight;
                this.graphElements.svg.attr("width", width).attr("height", height);
                this.simulation.force("center", d3.forceCenter(width / 2, height / 2));
                this.simulation.restart();
            }
        }
    }

    showLoading() {
        const loadingElements = document.querySelectorAll('.chart-container, .metric-card');
        loadingElements.forEach(el => el.classList.add('loading'));
    }

    hideLoading() {
        const loadingElements = document.querySelectorAll('.chart-container, .metric-card');
        loadingElements.forEach(el => el.classList.remove('loading'));
    }

    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `alert alert-${type} notification`;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
            opacity: 0;
            transform: translateX(100%);
            transition: all 0.3s ease;
        `;
        notification.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'exclamation' : 'info'}-circle"></i>
            ${message}
            <button type="button" class="btn-close" onclick="this.parentElement.remove()"></button>
        `;

        document.body.appendChild(notification);

        // Animate in
        setTimeout(() => {
            notification.style.opacity = '1';
            notification.style.transform = 'translateX(0)';
        }, 100);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.style.opacity = '0';
                notification.style.transform = 'translateX(100%)';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    updateCharts() {
        // Update all charts with new data
        Object.values(this.charts).forEach(chart => {
            if (chart && chart.update) {
                chart.update();
            }
        });
    }

    getProjectStats() {
        return {
            classes: document.querySelector('.metric-card h2')?.textContent || '0',
            methods: document.querySelectorAll('.metric-card h2')[1]?.textContent || '0',
            interfaces: document.querySelectorAll('.metric-card h2')[2]?.textContent || '0',
            linesOfCode: document.querySelectorAll('.metric-card h2')[3]?.textContent || '0'
        };
    }

    getChartsData() {
        return {
            complexity: this.charts.complexity?.data || null,
            quality: this.charts.quality?.data || null,
            duplication: this.charts.duplication?.data || null
        };
    }

    // Mock data generators for when server data is not available
    generateMockComplexityData() {
        return {
            labels: ['UserService', 'OrderController', 'ProductRepository', 'PaymentService', 'EmailService'],
            datasets: [{
                label: 'Cyclomatic Complexity',
                data: [12, 8, 15, 6, 10],
                backgroundColor: 'rgba(79, 172, 254, 0.8)',
                borderColor: 'rgba(79, 172, 254, 1)',
                borderWidth: 2
            }]
        };
    }

    generateMockQualityData() {
        return {
            labels: ['Maintainability', 'Complexity', 'Coverage', 'Duplication', 'Documentation'],
            datasets: [{
                label: 'Quality Metrics',
                data: [85, 70, 60, 90, 75],
                backgroundColor: 'rgba(79, 172, 254, 0.2)',
                borderColor: 'rgba(79, 172, 254, 1)',
                borderWidth: 2
            }]
        };
    }

    generateMockDuplicationData() {
        return {
            labels: ['Unique Code', 'Duplicated Code'],
            datasets: [{
                data: [85, 15],
                backgroundColor: ['rgba(75, 192, 192, 0.8)', 'rgba(255, 99, 132, 0.8)'],
                borderColor: ['rgba(75, 192, 192, 1)', 'rgba(255, 99, 132, 1)'],
                borderWidth: 2
            }]
        };
    }

    generateMockTrendData() {
        return {
            labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5'],
            datasets: [{
                label: 'Code Quality',
                data: [78, 82, 85, 83, 87],
                borderColor: 'rgba(79, 172, 254, 1)',
                backgroundColor: 'rgba(79, 172, 254, 0.1)',
                tension: 0.4
            }, {
                label: 'Test Coverage',
                data: [65, 68, 72, 75, 78],
                borderColor: 'rgba(75, 192, 192, 1)',
                backgroundColor: 'rgba(75, 192, 192, 0.1)',
                tension: 0.4
            }]
        };
    }

    generateMockGraphData() {
        return {
            nodes: [
                { id: 'UserService', name: 'UserService', methods: 8, fields: 3, package: 'com.example.service' },
                { id: 'OrderController', name: 'OrderController', methods: 6, fields: 2, package: 'com.example.controller' },
                { id: 'ProductRepository', name: 'ProductRepository', methods: 4, fields: 1, package: 'com.example.repository' },
                { id: 'PaymentService', name: 'PaymentService', methods: 5, fields: 2, package: 'com.example.service' }
            ],
            links: [
                { source: 'OrderController', target: 'UserService' },
                { source: 'OrderController', target: 'ProductRepository' },
                { source: 'OrderController', target: 'PaymentService' },
                { source: 'UserService', target: 'ProductRepository' }
            ]
        };
    }
}

// Initialize dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.parserDashboard = new ParserDashboard();
    
    // Expose control methods globally for button handlers
    window.resetZoom = () => window.parserDashboard.resetZoom();
    window.centerGraph = () => window.parserDashboard.centerGraph();
    window.togglePhysics = () => window.parserDashboard.togglePhysics();
});

// Utility functions
function formatNumber(num) {
    return new Intl.NumberFormat().format(num);
}

function formatBytes(bytes) {
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    if (bytes === 0) return '0 Bytes';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
}

function formatDuration(ms) {
    if (ms < 1000) return ms + 'ms';
    if (ms < 60000) return (ms / 1000).toFixed(1) + 's';
    return (ms / 60000).toFixed(1) + 'm';
}