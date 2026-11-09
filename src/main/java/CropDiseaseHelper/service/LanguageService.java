package CropDiseaseHelper.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LanguageService {

    private final Map<String, String> hindiTranslations = Map.ofEntries(

            Map.entry("Tomato - Early blight", "टमाटर - अगेती झुलसा"),
            Map.entry("Tomato - Late blight", "टमाटर - पछेती झुलसा"),
            Map.entry("Tomato - Bacterial spot", "टमाटर - बैक्टीरियल स्पॉट"),
            Map.entry("Tomato - healthy", "टमाटर - स्वस्थ"),

            Map.entry("Potato - Early blight", "आलू - अगेती झुलसा"),
            Map.entry("Potato - Late blight", "आलू - पछेती झुलसा"),
            Map.entry("Potato - healthy", "आलू - स्वस्थ")
    );

    public String translateDisease(String disease, String language) {

        if ("hi".equalsIgnoreCase(language)) {
            return hindiTranslations.getOrDefault(disease, disease);
        }

        return disease;
    }
    public String translateAdvice(String disease, String language) {

        if (!"hi".equalsIgnoreCase(language)) {
            return null;
        }

        return switch (disease) {

            case "Tomato - Early blight" ->
                    "जहाँ उपयुक्त हो, रोग प्रतिरोधी टमाटर की किस्मों का उपयोग करें। अपने क्षेत्र के अनुसार सही प्रबंधन के लिए स्थानीय कृषि विशेषज्ञ से सलाह लें।";

            case "Tomato - Late blight" ->
                    "संक्रमित पौधों के हिस्सों को हटा दें। पौधों के बीच पर्याप्त दूरी रखें, खेत में जल निकासी अच्छी रखें और फसल के आसपास अधिक नमी से बचें।";

            case "Potato - Late blight" ->
                    "स्वस्थ और रोगमुक्त बीज आलू का उपयोग करें। संक्रमित पौधों और संक्रमण के अन्य स्रोतों को हटाएँ। ठंडे और नम मौसम में खेत की नियमित निगरानी करें।";

            case "Tomato - healthy" ->
                    "मॉडल ने बीमारी के लक्षणों की पहचान नहीं की है। फसल की नियमित निगरानी करें और खेत में स्वच्छता बनाए रखें।";

            case "Potato - healthy" ->
                    "मॉडल ने बीमारी के लक्षणों की पहचान नहीं की है। स्वस्थ बीज आलू का उपयोग करें और फसल की नियमित निगरानी करें।";

            default ->
                    "इस बीमारी के लिए सत्यापित सलाह अभी उपलब्ध नहीं है। कृपया अपने नजदीकी कृषि विज्ञान केंद्र (KVK) से संपर्क करें।";
        };
    }
    public String translateUncertainMessage(String language) {

        if ("hi".equalsIgnoreCase(language)) {
            return "हम इस फसल की बीमारी की विश्वसनीय पहचान नहीं कर सके। "
                    + "कृपया एक स्पष्ट तस्वीर अपलोड करें या अपने नजदीकी "
                    + "कृषि विज्ञान केंद्र (KVK) से संपर्क करें।";
        }

        return "We could not confidently identify a disease for the selected crop. "
                + "Please upload a clearer image or contact your nearest "
                + "Krishi Vigyan Kendra (KVK).";
    }
}