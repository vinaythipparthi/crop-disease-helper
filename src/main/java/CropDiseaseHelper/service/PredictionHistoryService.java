package CropDiseaseHelper.service;

import CropDiseaseHelper.entity.PredictionHistory;
import CropDiseaseHelper.repository.PredictionHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PredictionHistoryService {

    private final PredictionHistoryRepository repository;

    public PredictionHistoryService(PredictionHistoryRepository repository) {
        this.repository = repository;
    }

    public PredictionHistory savePrediction(PredictionHistory history) {
        return repository.save(history);
    }

    public List<PredictionHistory> getAllPredictions() {
        return repository.findAll();
    }
}