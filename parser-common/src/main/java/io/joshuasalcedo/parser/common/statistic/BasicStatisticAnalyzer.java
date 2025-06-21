package io.joshuasalcedo.parser.common.statistic;


import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.BasicStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

public final class BasicStatisticAnalyzer implements Analyzer<BasicStatistics, List<Double>> {
    
    @Override
    public BasicStatistics analyze(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return new BasicStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(stats::addValue);
        
        return new BasicStatistics(
            stats.getN(),
            stats.getMean(),
            stats.getStandardDeviation(),
            stats.getVariance(),
            stats.getMin(),
            stats.getMax(),
            stats.getSum(),
            stats.getPercentile(25),
            stats.getPercentile(50),
            stats.getPercentile(75),
            stats.getSkewness()
        );
    }
}

