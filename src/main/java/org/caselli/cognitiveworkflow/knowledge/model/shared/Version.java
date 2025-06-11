package org.caselli.cognitiveworkflow.knowledge.model.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Version {
    @NotNull
    @JsonProperty("major")
    private int major;

    @NotNull
    @JsonProperty("minor")
    private int minor;

    @NotNull
    @JsonProperty("patch")
    private int patch;

    @JsonProperty("label")
    private String label;

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (label != null ? "-" + label : "");
    }

    
    /**
     * Determines if version changes are backward compatible
     * - Major version changes = breaking changes
     * - Minor/patch = backward compatible
     */
    public boolean isCompatible(Version oldVersion, Version newVersion) {
        return oldVersion.getMajor() == newVersion.getMajor() &&
                (oldVersion.getMinor() < newVersion.getMinor() ||
                        (oldVersion.getMinor() == newVersion.getMinor() &&
                                oldVersion.getPatch() <= newVersion.getPatch()));
    }

    /**
     * Creates a new version based on change type
     */
    public Version incrementVersion(Version currentVersion, ChangeType changeType) {
        Version newVersion = new Version();
        newVersion.setMajor(currentVersion.getMajor());
        newVersion.setMinor(currentVersion.getMinor());
        newVersion.setPatch(currentVersion.getPatch());

        switch (changeType) {
            case MAJOR:
                newVersion.setMajor(currentVersion.getMajor() + 1);
                newVersion.setMinor(0);
                newVersion.setPatch(0);
                break;
            case MINOR:
                newVersion.setMinor(currentVersion.getMinor() + 1);
                newVersion.setPatch(0);
                break;
            case PATCH:
                newVersion.setPatch(currentVersion.getPatch() + 1);
                break;
        }

        return newVersion;
    }

    /**
     * Checks if first version is greater than second version
     */
    public static boolean isGreaterThan(Version v1, Version v2) {
        if (v1 == null || v2 == null) return false;

        if (v1.major != v2.major) return v1.major > v2.major;
        if (v1.minor != v2.minor) return v1.minor > v2.minor;
        return v1.patch > v2.patch;
    }



    public enum ChangeType {
            MAJOR,
            MINOR,
            PATCH
    }
}
