package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.TimeSeriesStatistics;
import tech.tablesaw.api.*;

import java.util.*;

// ====== TIME SERIES ANALYZER ======
public class TimeSeriesAnalyzer implements Analyzer<TimeSeriesStatistics, Table> {
    
    @Override
    public TimeSeriesStatistics analyze(Table timeSeriesData) {
        if (timeSeriesData == null || timeSeriesData.isEmpty()) {
            return new TimeSeriesStatistics(0.0, 0.0, new double[]{}, new double[]{}, 0.0, 0.0);
        }
        
        // Assuming first column is time and second is value
        NumericColumn<?>[] numericColumns = timeSeriesData.numberColumns();
        if (numericColumns.length == 0) {
            return new TimeSeriesStatistics(0.0, 0.0, new double[]{}, new double[]{}, 0.0, 0.0);
        }
        NumericColumn<?> values = numericColumns[0];
        double[] data = values.asDoubleArray();
        
        // Calculate trend using simple linear regression
        double trend = calculateTrend(data);
        
        // Calculate moving average (window size = 3)
        double[] movingAverage = calculateMovingAverage(data, 3);
        
        // Calculate exponential smoothing (alpha = 0.3)
        double[] exponentialSmoothing = calculateExponentialSmoothing(data, 0.3);
        
        // Calculate volatility (standard deviation of returns)
        double volatility = calculateVolatility(data);
        
        // Calculate autocorrelation at lag 1
        double autocorrelation = calculateAutocorrelation(data, 1);
        
        // Seasonality would require more complex analysis
        double seasonality = 0.0; // Placeholder
        
        return new TimeSeriesStatistics(
            trend,
            seasonality,
            movingAverage,
            exponentialSmoothing,
            volatility,
            autocorrelation
        );
    }
    
    private double calculateTrend(double[] data) {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = data.length;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += data[i];
            sumXY += i * data[i];
            sumX2 += i * i;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    private double[] calculateMovingAverage(double[] data, int window) {
        double[] ma = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            double sum = 0;
            int count = 0;
            for (int j = Math.max(0, i - window + 1); j <= i; j++) {
                sum += data[j];
                count++;
            }
            ma[i] = sum / count;
        }
        return ma;
    }
    
    private double[] calculateExponentialSmoothing(double[] data, double alpha) {
        double[] smoothed = new double[data.length];
        smoothed[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            smoothed[i] = alpha * data[i] + (1 - alpha) * smoothed[i - 1];
        }
        return smoothed;
    }
    
    private double calculateVolatility(double[] data) {
        if (data.length < 2) return 0.0;
        
        double[] returns = new double[data.length - 1];
        for (int i = 1; i < data.length; i++) {
            returns[i - 1] = (data[i] - data[i - 1]) / data[i - 1];
        }
        
        double mean = Arrays.stream(returns).average().orElse(0.0);
        double variance = Arrays.stream(returns)
            .map(r -> Math.pow(r - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private double calculateAutocorrelation(double[] data, int lag) {
        if (lag >= data.length) return 0.0;
        
        double mean = Arrays.stream(data).average().orElse(0.0);
        double variance = Arrays.stream(data)
            .map(x -> Math.pow(x - mean, 2))
            .average().orElse(0.0);
        
        if (variance == 0) return 0.0;
        
        double covariance = 0;
        for (int i = lag; i < data.length; i++) {
            covariance += (data[i] - mean) * (data[i - lag] - mean);
        }
        covariance /= (data.length - lag);
        
        return covariance / variance;
    }
}

