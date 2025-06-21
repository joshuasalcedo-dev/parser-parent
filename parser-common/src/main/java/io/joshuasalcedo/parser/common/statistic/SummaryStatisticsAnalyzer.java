package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.Map;

public class SummaryStatisticsAnalyzer implements Analyzer<SummaryStatistics, double[]> {

    @Override
    public SummaryStatistics analyze(double[] data) {
        if (data == null || data.length == 0) {
            return new SummaryStatistics(Map.of(), Map.of(), Map.of(), Map.of(), 0);
        }

        Map<String, Double> centralTendency = new HashMap<>();
        Map<String, Double> dispersion = new HashMap<>();
        Map<String, Double> shape = new HashMap<>();
        Map<String, Double> percentiles = new HashMap<>();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (double value : data) {
            stats.addValue(value);
        }

        // Calculate central tendency measures
        centralTendency.put("mean", stats.getMean());
        centralTendency.put("median", stats.getPercentile(50));

        // Calculate dispersion measures
        dispersion.put("stdDev", stats.getStandardDeviation());
        dispersion.put("variance", stats.getVariance());
        dispersion.put("range", stats.getMax() - stats.getMin());

        // Calculate shape measures
        shape.put("skewness", stats.getSkewness());
        shape.put("kurtosis", stats.getKurtosis());

        // Calculate percentiles
        percentiles.put("p10", stats.getPercentile(10));
        percentiles.put("p25", stats.getPercentile(25));
        percentiles.put("p50", stats.getPercentile(50));
        percentiles.put("p75", stats.getPercentile(75));
        percentiles.put("p90", stats.getPercentile(90));

        return new SummaryStatistics(
            centralTendency,
            dispersion,
            shape,
            percentiles,
            stats.getN()
        );
    }
}
