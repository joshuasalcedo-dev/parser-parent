package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.ChartData;
import io.joshuasalcedo.parser.common.model.ChartGenerationResult;
import io.joshuasalcedo.parser.common.model.SeriesData;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ====== CHART GENERATOR ANALYZER ======
public class ChartGeneratorAnalyzer implements Analyzer<ChartGenerationResult, ChartData> {
    
    @Override
    public ChartGenerationResult analyze(ChartData data) {
        if (data == null) {
            return new ChartGenerationResult(null, "No data", false, Map.of());
        }
        
        try {
            XYChart chart = null;
            String chartPath = "";
            Map<String, Object> metadata = new HashMap<>();
            
            switch (data.chartType()) {
                case "line" -> {
                    chart = new XYChartBuilder()
                        .width(800)
                        .height(600)
                        .title(data.title())
                        .xAxisTitle(data.xLabel())
                        .yAxisTitle(data.yLabel())
                        .build();
                    
                    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
                    
                    for (int i = 0; i < data.series().size(); i++) {
                        SeriesData series = data.series().get(i);
                        chart.addSeries(series.name(), series.xData(), series.yData());
                    }
                    
                    chartPath = "charts/" + data.title().replaceAll(" ", "_") + "_line.png";
                    BitmapEncoder.saveBitmap(chart, chartPath, BitmapEncoder.BitmapFormat.PNG);
                }
                
                case "bar" -> {
                    CategoryChart barChart = new CategoryChartBuilder()
                        .width(800)
                        .height(600)
                        .title(data.title())
                        .xAxisTitle(data.xLabel())
                        .yAxisTitle(data.yLabel())
                        .build();
                    
                    SeriesData series = data.series().get(0);
                    List<String> categories = series.xData().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                    
                    barChart.addSeries(series.name(), categories, series.yData());
                    
                    chartPath = "charts/" + data.title().replaceAll(" ", "_") + "_bar.png";
                    BitmapEncoder.saveBitmap(barChart, chartPath, BitmapEncoder.BitmapFormat.PNG);
                }
                
                case "pie" -> {
                    PieChart pieChart = new PieChartBuilder()
                        .width(800)
                        .height(600)
                        .title(data.title())
                        .build();
                    
                    SeriesData series = data.series().get(0);
                    for (int i = 0; i < series.xData().size(); i++) {
                        pieChart.addSeries(
                            String.valueOf(series.xData().get(i)),
                            series.yData().get(i)
                        );
                    }
                    
                    chartPath = "charts/" + data.title().replaceAll(" ", "_") + "_pie.png";
                    BitmapEncoder.saveBitmap(pieChart, chartPath, BitmapEncoder.BitmapFormat.PNG);
                }
                
                case "scatter" -> {
                    chart = new XYChartBuilder()
                        .width(800)
                        .height(600)
                        .title(data.title())
                        .xAxisTitle(data.xLabel())
                        .yAxisTitle(data.yLabel())
                        .build();
                    
                    chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
                    
                    for (SeriesData series : data.series()) {
                        chart.addSeries(series.name(), series.xData(), series.yData());
                    }
                    
                    chartPath = "charts/" + data.title().replaceAll(" ", "_") + "_scatter.png";
                    BitmapEncoder.saveBitmap(chart, chartPath, BitmapEncoder.BitmapFormat.PNG);
                }
            }
            
            metadata.put("chartType", data.chartType());
            metadata.put("seriesCount", data.series().size());
            metadata.put("totalDataPoints", data.series().stream()
                .mapToInt(s -> s.xData().size())
                .sum());
            
            return new ChartGenerationResult(chartPath, "Chart generated successfully", true, metadata);
            
        } catch (Exception e) {
            return new ChartGenerationResult(null, "Error: " + e.getMessage(), false, Map.of());
        }
    }
}
