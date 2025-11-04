package jdbc;

public enum Role {
    PATIENT,
    DOCTOR;

    public static Role fromString(String s) {
        if (s == null) return PATIENT;
        try {
            return Role.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PATIENT;
        }
    }
}