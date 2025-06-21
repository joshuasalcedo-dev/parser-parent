package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.model.BasicStatistics;
import io.joshuasalcedo.parser.common.model.ChartData;
import io.joshuasalcedo.parser.common.model.ChartGenerationResult;
import io.joshuasalcedo.parser.common.model.CorrelationAnalysis;
import io.joshuasalcedo.parser.common.model.DataQualityMetrics;
import io.joshuasalcedo.parser.common.model.DistributionAnalysis;
import io.joshuasalcedo.parser.common.model.MultivariateStatistics;
import io.joshuasalcedo.parser.common.model.OutlierAnalysis;
import io.joshuasalcedo.parser.common.model.RegressionAnalysis;
import io.joshuasalcedo.parser.common.model.StatisticalTestResult;
import io.joshuasalcedo.parser.common.model.SummaryStatistics;
import io.joshuasalcedo.parser.common.model.TimeSeriesStatistics;
import tech.tablesaw.api.Table;

import java.util.List;

/**
 * A fluent API entry point for statistical analysis operations.
 * This class provides a convenient way to access various statistical analyzers.
 */
public final class StatisticAnalyzer {

    private StatisticAnalyzer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Entry point for basic statistical analysis.
     * 
     * @return A fluent interface for basic statistics operations
     */
    public static BasicStatisticsBuilder forBasicStatistics() {
        return new BasicStatisticsBuilder();
    }

    /**
     * Entry point for correlation analysis.
     * 
     * @return A fluent interface for correlation operations
     */
    public static CorrelationBuilder forCorrelation() {
        return new CorrelationBuilder();
    }

    /**
     * Entry point for chart generation.
     * 
     * @return A fluent interface for chart generation operations
     */
    public static ChartBuilder forChartGeneration() {
        return new ChartBuilder();
    }

    /**
     * Entry point for time series analysis.
     * 
     * @return A fluent interface for time series operations
     */
    public static TimeSeriesBuilder forTimeSeries() {
        return new TimeSeriesBuilder();
    }

    /**
     * Entry point for distribution analysis.
     * 
     * @return A fluent interface for distribution analysis operations
     */
    public static DistributionBuilder forDistribution() {
        return new DistributionBuilder();
    }

    /**
     * Entry point for data quality analysis.
     * 
     * @return A fluent interface for data quality analysis operations
     */
    public static DataQualityBuilder forDataQuality() {
        return new DataQualityBuilder();
    }

    /**
     * Entry point for multivariate analysis.
     * 
     * @return A fluent interface for multivariate analysis operations
     */
    public static MultivariateBuilder forMultivariate() {
        return new MultivariateBuilder();
    }

    /**
     * Entry point for outlier analysis.
     * 
     * @return A fluent interface for outlier analysis operations
     */
    public static OutlierBuilder forOutliers() {
        return new OutlierBuilder();
    }

    /**
     * Entry point for regression analysis.
     * 
     * @return A fluent interface for regression analysis operations
     */
    public static RegressionBuilder forRegression() {
        return new RegressionBuilder();
    }

    /**
     * Entry point for statistical hypothesis testing.
     * 
     * @return A fluent interface for statistical test operations
     */
    public static StatisticalTestBuilder forStatisticalTest() {
        return new StatisticalTestBuilder();
    }

    /**
     * Entry point for summary statistics.
     * 
     * @return A fluent interface for summary statistics operations
     */
    public static SummaryStatisticsBuilder forSummaryStatistics() {
        return new SummaryStatisticsBuilder();
    }

    /**
     * Builder for basic statistics operations.
     */
    public static class BasicStatisticsBuilder {
        private final BasicStatisticAnalyzer analyzer = new BasicStatisticAnalyzer();

        /**
         * Analyzes the provided data to compute basic statistics.
         * 
         * @param data The list of double values to analyze
         * @return The computed basic statistics
         */
        public BasicStatistics analyze(List<Double> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for correlation analysis operations.
     */
    public static class CorrelationBuilder {
        private final CorrelationAnalyzer analyzer = new CorrelationAnalyzer();

        /**
         * Analyzes the provided data to compute correlation statistics.
         * 
         * @param data The list of double arrays to analyze
         * @return The computed correlation analysis
         */
        public CorrelationAnalysis analyze(List<double[]> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for chart generation operations.
     */
    public static class ChartBuilder {
        private final ChartGeneratorAnalyzer analyzer = new ChartGeneratorAnalyzer();

        /**
         * Generates a chart based on the provided chart data.
         * 
         * @param data The chart data to visualize
         * @return The chart generation result
         */
        public ChartGenerationResult generate(ChartData data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for time series analysis operations.
     */
    public static class TimeSeriesBuilder {
        private final TimeSeriesAnalyzer analyzer = new TimeSeriesAnalyzer();

        /**
         * Analyzes the provided time series data.
         * 
         * @param data The time series data to analyze
         * @return The computed time series statistics
         */
        public TimeSeriesStatistics analyze(Table data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for distribution analysis operations.
     */
    public static class DistributionBuilder {
        private final DistributionAnalyzer analyzer = new DistributionAnalyzer();

        /**
         * Analyzes the provided data to compute distribution statistics.
         * 
         * @param data The list of double values to analyze
         * @return The computed distribution analysis
         */
        public DistributionAnalysis analyze(List<Double> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for data quality analysis operations.
     */
    public static class DataQualityBuilder {
        private final DataQualityAnalyzer analyzer = new DataQualityAnalyzer();

        /**
         * Analyzes the provided data to assess its quality.
         * 
         * @param data The table data to analyze
         * @return The computed data quality metrics
         */
        public DataQualityMetrics analyze(Table data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for multivariate analysis operations.
     */
    public static class MultivariateBuilder {
        private final MultivariateAnalyzer analyzer = new MultivariateAnalyzer();

        /**
         * Analyzes the provided data to compute multivariate statistics.
         * 
         * @param data The 2D array of doubles representing multivariate data
         * @return The computed multivariate statistics
         */
        public MultivariateStatistics analyze(double[][] data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for outlier analysis operations.
     */
    public static class OutlierBuilder {
        private final OutlierAnalyzer analyzer = new OutlierAnalyzer();

        /**
         * Analyzes the provided data to identify outliers.
         * 
         * @param data The list of double values to analyze
         * @return The computed outlier analysis
         */
        public OutlierAnalysis analyze(List<Double> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for regression analysis operations.
     */
    public static class RegressionBuilder {
        private final RegressionAnalyzer analyzer = new RegressionAnalyzer();

        /**
         * Analyzes the provided data to compute regression statistics.
         * 
         * @param data The list of double arrays to analyze
         * @return The computed regression analysis
         */
        public RegressionAnalysis analyze(List<double[]> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for statistical test operations.
     */
    public static class StatisticalTestBuilder {
        private final StatisticalTestAnalyzer analyzer = new StatisticalTestAnalyzer();

        /**
         * Analyzes the provided data to perform statistical hypothesis testing.
         * 
         * @param data The list of double arrays to analyze
         * @return The computed statistical test result
         */
        public StatisticalTestResult analyze(List<double[]> data) {
            return analyzer.analyze(data);
        }
    }

    /**
     * Builder for summary statistics operations.
     */
    public static class SummaryStatisticsBuilder {
        private final SummaryStatisticsAnalyzer analyzer = new SummaryStatisticsAnalyzer();

        /**
         * Analyzes the provided data to compute summary statistics.
         * 
         * @param data The primitive double array to analyze
         * @return The computed summary statistics
         */
        public SummaryStatistics analyze(double[] data) {
            return analyzer.analyze(data);
        }
    }
}
