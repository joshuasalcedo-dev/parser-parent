package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.OutlierAnalysis;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;

public class OutlierAnalyzer implements Analyzer<OutlierAnalysis, List<Double>> {

    @Override
    public OutlierAnalysis analyze(List<Double> data) {
        if (data == null || data.isEmpty()) {
            return new OutlierAnalysis(List.of(), List.of(), 0.0, 0.0, 0.0, "IQR");
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(stats::addValue);

        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        List<Double> outliers = new ArrayList<>();
        List<Integer> outlierIndices = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            double value = data.get(i);
            if (value < lowerBound || value > upperBound) {
                outliers.add(value);
                outlierIndices.add(i);
            }
        }

        return new OutlierAnalysis(
            outliers,
            outlierIndices,
            lowerBound,
            upperBound,
            iqr,
            "IQR"
        );
    }
}