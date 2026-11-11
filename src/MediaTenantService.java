import java.io.IOException;
import java.util.Objects;

/** Coordinates the course-media lifecycle around one account key. */
public final class MediaTenantService {
    public record Tenant(String tenantId, String email, String keyId, String userId, String keySecret) {}

    private final InfraiClient infrai;

    public MediaTenantService(InfraiClient infrai) {
        this.infrai = Objects.requireNonNull(infrai);
    }

    public Tenant provision(String tenantId, String email, String name) throws IOException, InterruptedException {
        String keyEnvelope = infrai.post("/v1/account/keys/create",
                "{\"project_id\":" + JsonStrings.quote(tenantId) + ",\"name\":" + JsonStrings.quote("course-media-" + tenantId)
                        + ",\"scopes\":[\"media\"],\"idempotency_key\":" + JsonStrings.quote("tenant-" + tenantId) + "}");
        String keyId = JsonStrings.extract(keyEnvelope, "key_id");
        String keySecret = JsonStrings.extract(keyEnvelope, "key_secret");
        String userEnvelope = infrai.post("/v1/auth/user/create",
                "{\"email\":" + JsonStrings.quote(email) + ",\"name\":" + JsonStrings.quote(name)
                        + ",\"metadata\":{\"tenant_id\":" + JsonStrings.quote(tenantId)
                        + "},\"idempotency_key\":" + JsonStrings.quote("user-" + tenantId) + "}");
        return new Tenant(tenantId, email, keyId, JsonStrings.extract(userEnvelope, "user_id"), keySecret);
    }

    public String revokeKey(String keyId) throws IOException, InterruptedException {
        return infrai.delete("/v1/account/keys/revoke/" + keyId);
    }

}
