package CropDiseaseHelper.ai;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import ai.onnxruntime.OnnxTensor;
import java.nio.FloatBuffer;
import java.util.Map;

import java.io.IOException;
import java.io.InputStream;

@Component
public class OnnxModelLoader {

    private OrtEnvironment environment;
    private OrtSession session;

    @PostConstruct
    public void loadModel() {

        try {
            environment = OrtEnvironment.getEnvironment();

            InputStream modelStream = getClass()
                    .getResourceAsStream("/models/cropguard.onnx");

            if (modelStream == null) {
                throw new IllegalStateException(
                        "ONNX model not found in resources/models"
                );
            }

            byte[] modelBytes;

            try (modelStream) {
                modelBytes = modelStream.readAllBytes();
            }

            session = environment.createSession(
                    modelBytes,
                    new OrtSession.SessionOptions()
            );

            System.out.println("ONNX model loaded successfully!");

            System.out.println("Model Inputs:");
            session.getInputInfo().forEach((name, info) ->
                    System.out.println(name + " : " + info.getInfo())
            );

            System.out.println("Model Outputs:");
            session.getOutputInfo().forEach((name, info) ->
                    System.out.println(name + " : " + info.getInfo())
            );

        } catch (IOException | OrtException e) {
            throw new IllegalStateException(
                    "Failed to load ONNX model", e
            );
        }
    }

    @PreDestroy
    public void closeModel() throws OrtException {

        if (session != null) {
            session.close();
        }

        System.out.println("ONNX model session closed.");
    }


    public float[] runInference(float[] input) throws OrtException {

        // Expected model input dimensions
        long[] shape = {1, 3, 224, 224};

        // Convert Java float array into an ONNX tensor
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(input),
                shape
        )) {

            // Execute the model
            try (OrtSession.Result result = session.run(
                    Map.of("input", inputTensor)
            )) {

                // Read the output named "logits"
                float[][] output = (float[][])
                        result.get("logits")
                                .orElseThrow()
                                .getValue();

                // Return scores for all 38 classes
                return output[0];
            }
        }
    }

}
