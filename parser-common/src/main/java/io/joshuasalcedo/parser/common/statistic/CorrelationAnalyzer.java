package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.CorrelationAnalysis;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.distribution.TDistribution;

import java.util.List;

// ====== CORRELATION ANALYZER ======
public class CorrelationAnalyzer implements Analyzer<CorrelationAnalysis, List<double[]>> {
    
    @Override
    public CorrelationAnalysis analyze(List<double[]> data) {
        if (data == null || data.size() < 2) {
            return new CorrelationAnalysis(0.0, 1.0, new double[][]{}, "none", false);
        }
        
        double[] x = data.get(0);
        double[] y = data.get(1);
        
        if (x.length != y.length || x.length < 2) {
            return new CorrelationAnalysis(0.0, 1.0, new double[][]{}, "none", false);
        }
        
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        double pearsonCoeff = correlation.correlation(x, y);
        
        // Calculate p-value (simplified)
        int n = x.length;
        double t = pearsonCoeff * Math.sqrt((n - 2) / (1 - pearsonCoeff * pearsonCoeff));
        TDistribution tDist = new TDistribution(n - 2);
        double pValue = 2 * (1 - tDist.cumulativeProbability(Math.abs(t)));
        
        // Correlation matrix for multiple variables
        double[][] correlationMatrix = null;
        if (data.size() > 2) {
            double[][] dataMatrix = data.toArray(new double[0][]);
            correlationMatrix = correlation.computeCorrelationMatrix(dataMatrix).getData();
        }
        
        String strength = getCorrelationStrength(Math.abs(pearsonCoeff));
        boolean isSignificant = pValue < 0.05;
        
        return new CorrelationAnalysis(
            pearsonCoeff,
            pValue,
            correlationMatrix != null ? correlationMatrix : new double[][]{{1.0, pearsonCoeff}, {pearsonCoeff, 1.0}},
            strength,
            isSignificant
        );
    }
    
    private String getCorrelationStrength(double absCorr) {
        if (absCorr >= 0.8) return "very strong";
        if (absCorr >= 0.6) return "strong";
        if (absCorr >= 0.4) return "moderate";
        if (absCorr >= 0.2) return "weak";
        return "very weak";
    }
}
