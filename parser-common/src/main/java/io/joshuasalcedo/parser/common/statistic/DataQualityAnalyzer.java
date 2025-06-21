package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.DataQualityMetrics;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataQualityAnalyzer implements Analyzer<DataQualityMetrics, Table> {
    
    @Override
    public DataQualityMetrics analyze(Table table) {
        if (table == null) {
            return new DataQualityMetrics(0, 0, 0, 0.0, Map.of(), List.of());
        }
        
        long totalRecords = table.rowCount();
        long missingValues = table.columns().stream()
            .mapToLong(Column::countMissing)
            .sum();
        
        // Check for duplicates (simplified - check first column)
        long duplicates = 0;
        if (!table.isEmpty()) {
            Column<?> firstCol = table.column(0);
            long uniqueValues = firstCol.unique().size();
            duplicates = totalRecords - uniqueValues;
        }
        
        double totalCells = totalRecords * table.columnCount();
        double completenessRatio = totalCells > 0 ? (totalCells - missingValues) / totalCells : 0.0;
        
        // Value frequencies for first column
        Map<String, Long> valueFrequencies = new HashMap<>();
        if (!table.isEmpty()) {
            Column<?> firstCol = table.column(0);
            for (Object value : firstCol.unique()) {
                long count = 0;
                for (int i = 0; i < firstCol.size(); i++) {
                    if (value.equals(firstCol.get(i))) {
                        count++;
                    }
                }
                valueFrequencies.put(String.valueOf(value), count);
            }
        }
        
        List<String> anomalies = new ArrayList<>();
        // Simple anomaly detection - could be enhanced
        if (missingValues > totalRecords * 0.1) {
            anomalies.add("High missing value rate: " + (missingValues * 100.0 / totalCells) + "%");
        }
        if (duplicates > totalRecords * 0.1) {
            anomalies.add("High duplicate rate: " + (duplicates * 100.0 / totalRecords) + "%");
        }
        
        return new DataQualityMetrics(
            totalRecords,
            missingValues,
            duplicates,
            completenessRatio,
            valueFrequencies,
            anomalies
        );
    }
}