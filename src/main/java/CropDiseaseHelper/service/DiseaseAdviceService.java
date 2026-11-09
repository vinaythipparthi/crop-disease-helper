
package CropDiseaseHelper.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiseaseAdviceService {

    private final Map<String, String> adviceMap = Map.ofEntries(

            Map.entry(
                    "Tomato - Early blight",
                    "Use disease-resistant tomato varieties where suitable. " +
                            "Consult your local agricultural expert for region-specific " +
                            "management recommendations."
            ),

            Map.entry(
                    "Tomato - Late blight",
                    "Remove infected plant parts, maintain sufficient spacing " +
                            "between plants, ensure good field drainage, and avoid " +
                            "excess moisture around the crop."
            ),

            Map.entry(
                    "Potato - Late blight",
                    "Use healthy, disease-free seed tubers. Remove infected " +
                            "plants and other sources of infection. Monitor the field " +
                            "regularly, especially during cool and humid weather."
            ),

            Map.entry(
                    "Tomato - healthy",
                    "No disease symptoms were identified by the model. " +
                            "Continue regular crop monitoring and good field hygiene."
            ),

            Map.entry(
                    "Potato - healthy",
                    "No disease symptoms were identified by the model. " +
                            "Continue regular crop monitoring and use healthy seed tubers."
            )
    );

    public String getAdvice(String diseaseName) {

        return adviceMap.getOrDefault(
                diseaseName,
                "Verified advice is not currently available for this " +
                        "prediction. Please consult your nearest Krishi Vigyan Kendra (KVK)."
        );
    }
    public String getAdviceSource(String diseaseName) {

        if (diseaseName.equals("Tomato - Early blight")
                || diseaseName.equals("Tomato - Late blight")) {

            return "ICAR - Technological Interventions in Tomato";
        }

        return "No verified disease-specific source available. " +
                "Please consult your nearest KVK.";
    }

    public String getAdviceSourceUrl(String diseaseName) {

        if (diseaseName.equals("Tomato - Early blight")
                || diseaseName.equals("Tomato - Late blight")) {

            return "https://icar.gov.in/en/node/4685";
        }

        return "";
    }
}
