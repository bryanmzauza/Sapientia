package dev.brmz.sapientia.api;

/** Semantic version of the Sapientia API. */
public record Version(int major, int minor, int patch) {

    public static Version parse(String raw) {
        String cleaned = raw.split("-", 2)[0];
        String[] parts = cleaned.split("\\.");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid version: " + raw);
        }
        return new Version(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
