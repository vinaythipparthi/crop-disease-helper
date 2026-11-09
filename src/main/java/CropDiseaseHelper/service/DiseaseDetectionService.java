package CropDiseaseHelper.service;

import CropDiseaseHelper.ai.ImagePreprocessor;
import CropDiseaseHelper.ai.OnnxModelLoader;
import ai.onnxruntime.OrtException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DiseaseDetectionService {

    private final ImagePreprocessor imagePreprocessor;
    private final OnnxModelLoader modelLoader;

    public DiseaseDetectionService(
            ImagePreprocessor imagePreprocessor,
            OnnxModelLoader modelLoader) {

        this.imagePreprocessor = imagePreprocessor;
        this.modelLoader = modelLoader;
    }

    public float[] detectDisease(MultipartFile file)
            throws IOException, OrtException {

        // Step 1: Convert image into model input
        float[] input = imagePreprocessor.preprocess(file);

        // Step 2: Run ONNX inference
        return modelLoader.runInference(input);
    }

    public boolean isSupportedCropPrediction(int predictedIndex, String crop) {

        if (crop.equals("tomato")) {
            return predictedIndex >= 28 && predictedIndex <= 37;
        }

        if (crop.equals("potato")) {
            return predictedIndex >= 20 && predictedIndex <= 22;
        }

        return false;
    }
}
