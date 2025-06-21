package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.RegressionAnalysis;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;

// ====== REGRESSION ANALYZER ======
public class RegressionAnalyzer implements Analyzer<RegressionAnalysis, List<double[]>> {
    
    @Override
    public RegressionAnalysis analyze(List<double[]> data) {
        if (data == null || data.size() < 2) {
            return new RegressionAnalysis(0.0, 0.0, 0.0, 0.0, new double[]{}, new double[]{});
        }
        
        double[] x = data.get(0);
        double[] y = data.get(1);
        
        if (x.length != y.length || x.length < 2) {
            return new RegressionAnalysis(0.0, 0.0, 0.0, 0.0, new double[]{}, new double[]{});
        }
        
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < x.length; i++) {
            regression.addData(x[i], y[i]);
        }
        
        double slope = regression.getSlope();
        double intercept = regression.getIntercept();
        double rSquared = regression.getRSquare();
        double standardError = regression.getSlopeStdErr();
        
        double[] predictedValues = new double[x.length];
        double[] residuals = new double[x.length];
        
        for (int i = 0; i < x.length; i++) {
            predictedValues[i] = regression.predict(x[i]);
            residuals[i] = y[i] - predictedValues[i];
        }
        
        return new RegressionAnalysis(
            slope,
            intercept,
            rSquared,
            standardError,
            residuals,
            predictedValues
        );
    }
}
