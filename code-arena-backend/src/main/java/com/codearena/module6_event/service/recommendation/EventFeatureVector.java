package com.codearena.module6_event.service.recommendation;

import com.codearena.module6_event.entity.ProgrammingEvent;
import java.util.Map;

public class EventFeatureVector {
    
    // Convert event properties to numerical features
    // Category weights
    public static final Map<String, double[]> CATEGORY_VECTORS = Map.of(
        "HACKATHON",   new double[]{1.0, 0.0, 0.0, 0.0, 0.0},
        "NETWORKING",  new double[]{0.0, 1.0, 0.0, 0.0, 0.0},
        "BOOTCAMP",    new double[]{0.0, 0.0, 1.0, 0.0, 0.0},
        "CONFERENCE",  new double[]{0.0, 0.0, 0.0, 1.0, 0.0},
        "REMISE_PRIX", new double[]{0.0, 0.0, 0.0, 0.0, 1.0}
    );

    // Type weights
    public static final Map<String, double[]> TYPE_VECTORS = Map.of(
        "OPEN",      new double[]{1.0, 0.0},
        "EXCLUSIVE", new double[]{0.0, 1.0}
    );

    // Convert event to feature vector
    public static double[] toVector(ProgrammingEvent event) {
        double[] category = CATEGORY_VECTORS.getOrDefault(
            event.getCategory().name(), 
            new double[]{0,0,0,0,0}
        );
        double[] type = TYPE_VECTORS.getOrDefault(
            event.getType().name(),
            new double[]{0,0}
        );
        
        // Fill rate feature (normalized 0-1)
        double fillRate = event.getMaxParticipants() == 0 ? 0 :
            (double) event.getCurrentParticipants() / 
            event.getMaxParticipants();
        
        // Combine all features into one vector
        // [cat0, cat1, cat2, cat3, cat4, type0, type1, fillRate]
        double[] vector = new double[8];
        System.arraycopy(category, 0, vector, 0, 5);
        System.arraycopy(type, 0, vector, 5, 2);
        vector[7] = fillRate;
        return vector;
    }
}
