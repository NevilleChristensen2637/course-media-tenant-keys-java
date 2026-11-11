import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MediaTenantServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> bodies = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String data = exchange.getRequestURI().getPath().contains("keys")
                    ? "{\"key_id\": \"key-test\", \"key_secret\": \"secret\\\"\\\\\\n\\u4e2d\"}"
                    : "{\"user_id\": \"user-test\"}";
            byte[] response = ("{\"ok\":true,\"data\":" + data + "}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String tenant = "course\"\\\n\t中";
            String email = "test\"\\@example.com";
            String name = "Studio\"\\\r\n\b\f\t\u0000😀";
            MediaTenantService service = new MediaTenantService(new InfraiClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "local-test"));
            MediaTenantService.Tenant result = service.provision(tenant, email, name);
            check(result.keyId().equals("key-test") && result.userId().equals("user-test"), "returned identifiers");
            check(result.keySecret().equals("secret\"\\\n中"), "one-time plaintext key");
            check(JsonStrings.extract(bodies.get(0), "project_id").equals(tenant), "tenant encoding");
            check(JsonStrings.extract(bodies.get(0), "name").equals("course-media-" + tenant), "key name encoding");
            check(JsonStrings.extract(bodies.get(1), "email").equals(email), "email encoding");
            check(JsonStrings.extract(bodies.get(1), "name").equals(name), "name encoding");
            check(JsonStrings.extract(bodies.get(1), "tenant_id").equals(tenant), "metadata encoding");
            for (String body : bodies) for (char c : body.toCharArray()) check(c >= 0x20, "raw control character");
            System.out.println("PASS: one tenant decision binds media ownership and user ownership");
        } finally {
            server.stop(0);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
