package io.joshuasalcedo.parser.common.statistic;


import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.DistributionAnalysis;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import java.util.*;

// ====== DISTRIBUTION ANALYZER ======
public class DistributionAnalyzer implements Analyzer<DistributionAnalysis, List<Double>> {

    @Override
    public DistributionAnalysis analyze(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return new DistributionAnalysis("Unknown", 0.0, 0.0, new double[]{0.0, 0.0}, 0.0, 0.0, false);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(stats::addValue);

        double mean = stats.getMean();
        double variance = stats.getVariance();
        double stdDev = stats.getStandardDeviation();

        // Calculate 95% confidence interval
        double confidenceLevel = 0.95;
        double criticalValue = 1.96; // for 95% confidence
        double standardError = stdDev / Math.sqrt(data.size());
        double[] confidenceInterval = new double[]{
            mean - criticalValue * standardError,
            mean + criticalValue * standardError
        };

        // Test for normality using Kolmogorov-Smirnov test
        KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
        
        // Create a random normal distribution sample for comparison
        double[] dataArray = data.stream().mapToDouble(Double::doubleValue).toArray();
        org.apache.commons.math3.distribution.NormalDistribution normalDist = 
            new org.apache.commons.math3.distribution.NormalDistribution(mean, stdDev);
        
        double[] normalSample = new double[dataArray.length];
        for (int i = 0; i < normalSample.length; i++) {
            normalSample[i] = normalDist.sample();
        }
        
        double ksStatistic = ksTest.kolmogorovSmirnovStatistic(dataArray, normalSample);
        double pValue = ksTest.kolmogorovSmirnovTest(dataArray, normalSample);

        String distributionType = pValue > 0.05 ? "Normal" : "Non-normal";
        boolean isNormallyDistributed = pValue > 0.05;

        return new DistributionAnalysis(
            distributionType,
            mean,
            variance,
            confidenceInterval,
            ksStatistic,
            pValue,
            isNormallyDistributed
        );
    }
}


