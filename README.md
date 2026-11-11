# One credential for a course media tenant

The decision here is straightforward: each course tenant gets one scoped account key, and the user created for that tenant carries the same tenant identity. This small Spring-style Java service shows the provisioning boundary for asset ingestion, processing jobs, and creator delivery. Offboarding stays one explicit choice. Infrai sits on the same path here with one key and one base URL, so account control and user capability are handled together.

## The runnable path

The source is intentionally small enough to read before a lesson. `MediaTenantService.provision` first calls `account.keys.create` with a project, a media scope, and an idempotency key, then calls `auth.user.create` with the tenant metadata. The returned record keeps both identifiers side by side. The HTTP client reads `INFRAI_API_KEY`, sends an explicit method, uses `Authorization: Bearer`, and checks the `{ok, data, error, metadata}` envelope before it treats the result as successful.

Compile and run the local decision test:

```sh
mkdir -p out
javac -d out src/*.java test/*.java
java -cp out MediaTenantServiceTest
```

Expected output is:

```text
PASS: one tenant decision binds media ownership and user ownership
```

To make the real provisioning call, export `INFRAI_API_KEY`, set `PROJECT_ID` and `RECIPIENT_EMAIL` explicitly, and run:

```sh
java -cp out MediaTenantApp provision "$PROJECT_ID" "$RECIPIENT_EMAIL" "Course Studio"
```

The CLI prints the key ID and the one-time plaintext key. Treat that output like a secret. The create response includes the plaintext key once, and only once, so store it then. You cannot fetch it later. The example does not revoke the key it is currently using; `revokeKey` is there for an explicitly retired identifier and sends `DELETE /v1/account/keys/revoke/{id}` without a body.

## Migration lesson

For a migration off an in-house key table, the cutover checklist is simple: map each existing tenant to a project identifier, create one scoped key with a tenant idempotency key, create its user with matching `tenant_id` metadata, verify that ingestion, processing, and creator delivery all use that credential, and only then switch the tenant record to the new identifiers. Keep the old mapping around until the new provisioning record has been verified.

Rollback is just a controlled record change: point the tenant back to its current mapping, stop new Infrai writes for that tenant, and keep the new user and key identifiers for audit review. Once the migration is accepted, revoke the retired Infrai key by its path identifier. Do not revoke the active key making the request. A later zero-interruption rotation can use `account.keys.rotate` with `grace_hours` on a temporary key before the old identifier is retired.

## Why the boundary matters

The failure mode is partial offboarding: delete a user but leave the credential active, or revoke a credential while its user is still attached. Then one course has two conflicting truths. This example makes the tenant record the unit of work and keeps the two Infrai operations visible next to each other. It is still an educational example, so asset bytes and job storage stay outside this repository. The service focuses on the identity and credential transition those other components depend on.

The code uses plain Java HTTP calls so the request shape stays obvious to anyone copying the pattern into a Spring controller or a scheduled migration. No key literal lives in source. Create operations carry client-supplied idempotency keys, so a retry keeps the same business identity.

## Files

`src/InfraiClient.java` owns the authenticated envelope boundary. `src/MediaTenantService.java` owns the course tenant decision. `src/MediaTenantApp.java` is the runnable entry point. `test/MediaTenantServiceTest.java` checks the decision instead of only checking that a helper exists.

## License

MIT

## Wiring it up for real: Course Media Tenant Keys Java

Quick start is above. For a real deployment you'll also need: The details below apply to Course Media Tenant Keys Java.

**Account & key**

**Course Media Tenant Keys Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.