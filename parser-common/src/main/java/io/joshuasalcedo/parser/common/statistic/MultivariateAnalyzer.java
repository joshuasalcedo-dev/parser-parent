package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.MultivariateStatistics;

import java.util.Arrays;

// ====== MULTIVARIATE ANALYZER ======
public class MultivariateAnalyzer implements Analyzer<MultivariateStatistics, double[][]> {

    @Override
    public MultivariateStatistics analyze(double[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            return new MultivariateStatistics(new double[][]{}, new double[][]{}, 
                new double[]{}, new double[][]{}, new double[]{}, 0.0);
        }

        int m = data.length;    // number of observations
        int n = data[0].length; // number of variables

        if (n < 2) {
            return new MultivariateStatistics(new double[][]{}, new double[][]{}, 
                new double[]{}, new double[][]{}, new double[]{}, 0.0);
        }

        // Calculate covariance matrix
        double[][] covarianceMatrix = calculateCovarianceMatrix(data);

        // Calculate correlation matrix
        double[][] correlationMatrix = calculateCorrelationMatrix(data);

        // For PCA, we would need more complex calculations
        // These are placeholder values
        double[] eigenvalues = new double[n];
        double[][] eigenVectors = new double[n][n];
        double[] principalComponents = new double[n];

        // Simple explained variance calculation
        double totalVariance = Arrays.stream(eigenvalues).sum();
        double explainedVariance = totalVariance > 0 ? eigenvalues[0] / totalVariance : 0.0;

        return new MultivariateStatistics(
            covarianceMatrix,
            correlationMatrix,
            eigenvalues,
            eigenVectors,
            principalComponents,
            explainedVariance
        );
    }

    private double[][] calculateCovarianceMatrix(double[][] data) {
        int n = data[0].length;
        double[][] covariance = new double[n][n];

        // Calculate means
        double[] means = new double[n];
        for (int j = 0; j < n; j++) {
            for (double[] row : data) {
                means[j] += row[j];
            }
            means[j] /= data.length;
        }

        // Calculate covariance
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (double[] row : data) {
                    sum += (row[i] - means[i]) * (row[j] - means[j]);
                }
                covariance[i][j] = sum / (data.length - 1);
            }
        }

        return covariance;
    }

    private double[][] calculateCorrelationMatrix(double[][] data) {
        int n = data[0].length;
        double[][] correlation = new double[n][n];
        double[][] covariance = calculateCovarianceMatrix(data);

        // Calculate standard deviations
        double[] stdDevs = new double[n];
        for (int i = 0; i < n; i++) {
            stdDevs[i] = Math.sqrt(covariance[i][i]);
        }

        // Calculate correlation
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (stdDevs[i] * stdDevs[j] > 0) {
                    correlation[i][j] = covariance[i][j] / (stdDevs[i] * stdDevs[j]);
                } else {
                    correlation[i][j] = 0;
                }
            }
        }

        return correlation;
    }
}
