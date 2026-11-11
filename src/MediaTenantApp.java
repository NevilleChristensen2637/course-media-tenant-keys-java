public final class MediaTenantApp {
    public static void main(String[] args) throws Exception {
        MediaTenantService service = new MediaTenantService(new InfraiClient());
        if (args.length == 0) {
            System.out.println("Set INFRAI_API_KEY, then run: provision <tenant> <email> <name>");
            return;
        }
        if (args.length != 4 || !args[0].equals("provision")) throw new IllegalArgumentException("provision requires tenant, email, name");
        MediaTenantService.Tenant tenant = service.provision(args[1], args[2], args[3]);
        System.out.println("Account key ID: " + tenant.keyId());
        System.out.println("Plaintext account key (store securely now): " + tenant.keySecret());
        System.out.println("Tenant " + tenant.tenantId() + " owns user " + tenant.userId() + "; store the plaintext key now, because it is returned once.");
    }
}
