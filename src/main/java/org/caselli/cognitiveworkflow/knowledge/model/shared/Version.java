package org.caselli.cognitiveworkflow.knowledge.model.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
     * Validates if a version bump follows semantic versioning rules
     * @param oldVersion the previous version
     * @param newVersion the new version to validate
     * @return true if the version bump is legitimate, false otherwise
     */
    public static boolean isValidVersionBump(Version oldVersion, Version newVersion) {
        if (oldVersion == null || newVersion == null) {
            return false;
        }

        // Allow same version numbers with label changes (e.g., 1.0.0-beta → 1.0.0)
        if (oldVersion.major == newVersion.major &&
                oldVersion.minor == newVersion.minor &&
                oldVersion.patch == newVersion.patch && !Objects.equals(oldVersion.label, newVersion.label)) {
            return true;
        }

        // Check for valid major version bump
        if (newVersion.major > oldVersion.major) {
            // Major increment should be +1
            if (newVersion.major != oldVersion.major + 1) return false;
            // Minor and patch should be reset to 0
            return newVersion.minor == 0 && newVersion.patch == 0;
        }

        // Check for valid minor version bump
        if (newVersion.minor > oldVersion.minor) {
            // Major should be unchanged
            if (newVersion.major != oldVersion.major) return false;
            // Minor increment should be exactly +1
            if (newVersion.minor != oldVersion.minor + 1) return false;
            // Patch should be reset to 0
            return newVersion.patch == 0;
        }

        // Check for valid patch version bump
        if (newVersion.patch > oldVersion.patch) {
            // Major and minor should be unchanged
            if (newVersion.major != oldVersion.major || newVersion.minor != oldVersion.minor) return false;
            // Patch increment should be +1
            return newVersion.patch == oldVersion.patch + 1;
        }

        return false;
    }


    public enum ChangeType {
            MAJOR,
            MINOR,
            PATCH
    }
}
