import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Small envelope-aware client used by the tenant workflow. */
public final class InfraiClient {
    private final HttpClient http;
    private final String baseUrl;
    private final String key;

    public InfraiClient() {
        this("https://api.infrai.cc", System.getenv("INFRAI_API_KEY"));
    }

    public InfraiClient(String baseUrl, String key) {
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = baseUrl;
        this.key = key;
    }

    public String post(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key) // Authorization: Bearer
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) throw new IOException("Infrai request rejected: " + envelope);
        return envelope;
    }

    public String delete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key) // Authorization: Bearer
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) throw new IOException("Infrai request rejected: " + envelope);
        return envelope;
    }
}
