package CropDiseaseHelper.service;

import org.springframework.stereotype.Service;

@Service
public class ConfidenceService {

    private static final double TEMPERATURE = 0.591;

    public double[] calculateProbabilities(float[] logits) {

        double[] probabilities = new double[logits.length];

        double maxLogit = Double.NEGATIVE_INFINITY;

        // Find the maximum logit
        for (float logit : logits) {
            maxLogit = Math.max(maxLogit, logit / TEMPERATURE);
        }

        double sum = 0.0;

        // Calculate exponentials
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = Math.exp(
                    (logits[i] / TEMPERATURE) - maxLogit
            );

            sum += probabilities[i];
        }

        // Normalize probabilities
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }

        return probabilities;
    }
    public boolean isConfident(double confidence) {
        return confidence >= 70.0;
    }
}