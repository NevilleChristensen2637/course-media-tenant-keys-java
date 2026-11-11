import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JSON string encoding and decoding for the small request/response boundary. */
final class JsonStrings {
    private JsonStrings() {}

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if (c < 0x20 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    static String extract(String json, String field) {
        Matcher match = Pattern.compile("\"" + Pattern.quote(field)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!match.find()) throw new IllegalArgumentException("Infrai response missing " + field);
        String encoded = match.group(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '\\') { out.append(c); continue; }
            char escape = encoded.charAt(++i);
            switch (escape) {
                case '"', '\\', '/' -> out.append(escape);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    out.append((char) Integer.parseInt(encoded.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
        return out.toString();
    }
}
