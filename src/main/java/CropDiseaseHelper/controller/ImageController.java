package CropDiseaseHelper.controller;

import CropDiseaseHelper.entity.PredictionHistory;
import CropDiseaseHelper.service.ConfidenceService;
import CropDiseaseHelper.service.DiseaseDetectionService;
import CropDiseaseHelper.service.DiseaseLabelService;
import CropDiseaseHelper.service.DiseaseAdviceService;
import CropDiseaseHelper.service.LanguageService;
import CropDiseaseHelper.service.PredictionHistoryService;

import ai.onnxruntime.OrtException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final Set<String> SUPPORTED_CROPS = Set.of(
            "tomato",
            "potato"
    );

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en",
            "hi"
    );

    private final DiseaseDetectionService diseaseDetectionService;
    private final DiseaseLabelService diseaseLabelService;
    private final ConfidenceService confidenceService;
    private final DiseaseAdviceService diseaseAdviceService;
    private final LanguageService languageService;
    private final PredictionHistoryService predictionHistoryService;

    public ImageController(
            DiseaseDetectionService diseaseDetectionService,
            DiseaseLabelService diseaseLabelService,
            ConfidenceService confidenceService,
            DiseaseAdviceService diseaseAdviceService,
            LanguageService languageService,
            PredictionHistoryService predictionHistoryService) {

        this.diseaseDetectionService = diseaseDetectionService;
        this.diseaseLabelService = diseaseLabelService;
        this.confidenceService = confidenceService;
        this.diseaseAdviceService = diseaseAdviceService;
        this.languageService = languageService;
        this.predictionHistoryService = predictionHistoryService;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getPredictionHistory() {
        return ResponseEntity.ok(
                predictionHistoryService.getAllPredictions()
        );
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predictDisease(
            @RequestParam("file") MultipartFile file,
            @RequestParam("crop") String crop,
            @RequestParam(value = "language", defaultValue = "en") String language) {

        crop = crop.toLowerCase().trim();
        language = language.toLowerCase().trim();

        // 1. Validate crop
        if (!SUPPORTED_CROPS.contains(crop)) {
            return ResponseEntity.badRequest()
                    .body("Currently, only tomato and potato are supported.");
        }

        // 2. Validate language
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            return ResponseEntity.badRequest()
                    .body("Supported languages are English and Hindi.");
        }

        // 3. Validate uploaded file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload an image!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                    .body("Image size must not exceed 5 MB!");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only JPEG and PNG images are allowed!");
        }

        try {
            // 4. Preprocess image and run ONNX inference
            float[] logits = diseaseDetectionService.detectDisease(file);

            // 5. Convert logits into probabilities
            double[] probabilities =
                    confidenceService.calculateProbabilities(logits);

            // 6. Find the top 5 predictions
            Integer[] indices = new Integer[probabilities.length];

            for (int i = 0; i < probabilities.length; i++) {
                indices[i] = i;
            }

            Arrays.sort(indices, (a, b) ->
                    Double.compare(probabilities[b], probabilities[a])
            );

            int predictedIndex = indices[0];
            double confidence = probabilities[predictedIndex] * 100;

            // ==================================
            // TOP 5 MODEL PREDICTIONS DEBUG
            // ==================================

            System.out.println("\n========== TOP 5 MODEL PREDICTIONS ==========");
            System.out.println("Selected crop: " + crop);
            System.out.println("Selected language: " + language);

            int topCount = Math.min(5, indices.length);

            for (int rank = 0; rank < topCount; rank++) {

                int index = indices[rank];

                String disease =
                        diseaseLabelService.getReadableDiseaseName(index);

                double score = probabilities[index] * 100;

                System.out.printf(
                        "%d. Index: %d | Disease: %s | Confidence: %.2f%%%n",
                        rank + 1,
                        index,
                        disease,
                        score
                );
            }

            System.out.println("=============================================\n");

            // 7. Get predicted disease name
            String predictedDisease =
                    diseaseLabelService.getReadableDiseaseName(predictedIndex);

            // 8. Check whether prediction belongs to selected crop
            boolean supportedCrop =
                    diseaseDetectionService.isSupportedCropPrediction(
                            predictedIndex, crop
                    );

            System.out.println("========== FINAL PREDICTION ==========");
            System.out.println("Predicted index: " + predictedIndex);
            System.out.println("Predicted disease: " + predictedDisease);
            System.out.println("Confidence: " + confidence + "%");
            System.out.println("Supported crop: " + supportedCrop);
            System.out.println("======================================");

            // 9. Handle crop mismatch
            if (!supportedCrop) {

                savePrediction(
                        crop,
                        predictedDisease,
                        confidence,
                        "UNCERTAIN",
                        language
                );

                return uncertainResponse(
                        language,
                        confidence,
                        "The model's top prediction does not match the selected crop."
                );
            }

            // 10. Check confidence threshold
            if (!confidenceService.isConfident(confidence)) {

                savePrediction(
                        crop,
                        predictedDisease,
                        confidence,
                        "UNCERTAIN",
                        language
                );

                return uncertainResponse(
                        language,
                        confidence,
                        "The prediction confidence is too low."
                );
            }

            // 11. Translate disease name
            String displayDisease =
                    languageService.translateDisease(
                            predictedDisease, language
                    );

            // 12. Get agricultural advice
            String advice =
                    diseaseAdviceService.getAdvice(predictedDisease);

            String translatedAdvice =
                    languageService.translateAdvice(
                            predictedDisease, language
                    );

            if (translatedAdvice != null) {
                advice = translatedAdvice;
            }

            // 13. Build successful response
            Map<String, Object> response = new LinkedHashMap<>();

            response.put("status", "SUCCESS");
            response.put("predictedDisease", displayDisease);
            response.put(
                    "confidence",
                    Math.round(confidence * 100.0) / 100.0
            );
            response.put("advice", advice);

            response.put(
                    "adviceSource",
                    diseaseAdviceService.getAdviceSource(predictedDisease)
            );

            response.put(
                    "adviceSourceUrl",
                    diseaseAdviceService.getAdviceSourceUrl(predictedDisease)
            );

            response.put(
                    "message",
                    "hi".equals(language)
                            ? "विश्लेषण पूरा हुआ।"
                            : "AI prediction completed successfully!"
            );

            // 14. Save successful prediction
            savePrediction(
                    crop,
                    predictedDisease,
                    confidence,
                    "SUCCESS",
                    language
            );

            return ResponseEntity.ok(response);

        } catch (IOException | OrtException e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Unable to process the image.");

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // Save prediction history in MySQL
    private void savePrediction(
            String crop,
            String disease,
            double confidence,
            String status,
            String language) {

        PredictionHistory history = new PredictionHistory();

        history.setCrop(crop);
        history.setPredictedDisease(disease);
        history.setConfidence(
                Math.round(confidence * 100.0) / 100.0
        );
        history.setStatus(status);
        history.setLanguage(language);

        predictionHistoryService.savePrediction(history);
    }

    // Returns an uncertain prediction in the selected language
    private ResponseEntity<Map<String, Object>> uncertainResponse(
            String language,
            double confidence,
            String reason) {

        boolean hindi = "hi".equals(language);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", "UNCERTAIN");

        response.put(
                "message",
                hindi
                        ? "हम इस फसल की बीमारी की विश्वसनीय पहचान नहीं कर सके। कृपया एक स्पष्ट तस्वीर अपलोड करें या अपने नजदीकी कृषि विज्ञान केंद्र (KVK) से संपर्क करें।"
                        : "We could not confidently identify a disease for the selected crop. Please upload a clearer image or contact your nearest Krishi Vigyan Kendra (KVK)."
        );

        response.put(
                "reason",
                hindi
                        ? "मॉडल की भविष्यवाणी चुनी गई फसल से मेल नहीं खाती या विश्वास स्तर कम है।"
                        : reason
        );

        response.put(
                "confidence",
                Math.round(confidence * 100.0) / 100.0
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please upload an image!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                    .body("Image size must not exceed 5 MB!");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only JPEG and PNG images are allowed!");
        }

        return ResponseEntity.ok(
                "Image uploaded successfully: " + file.getOriginalFilename()
        );
    }
}