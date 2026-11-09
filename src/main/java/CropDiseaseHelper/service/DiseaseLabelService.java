
package CropDiseaseHelper.service;

import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class DiseaseLabelService {

    private final List<String> labels;

    public DiseaseLabelService(ObjectMapper objectMapper) throws IOException {

        try (InputStream input = getClass()
                .getResourceAsStream("/models/classes.json")) {

            if (input == null) {
                throw new IllegalStateException("classes.json not found!");
            }

            labels = objectMapper.readValue(
                    input,
                    new TypeReference<List<String>>() {}
            );
        }
    }

    // Returns the original disease label using its index
    public String getDiseaseName(int index) {

        if (index < 0 || index >= labels.size()) {
            throw new IllegalArgumentException(
                    "Invalid disease class index"
            );
        }

        return labels.get(index);
    }

    // Converts the original label into a readable name
    public String getReadableDiseaseName(int index) {

        String disease = getDiseaseName(index);

        return disease
                .replace("___", " - ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
