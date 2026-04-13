package btw.modern;

public class CrashReport {
    private final String description;
    private final Throwable cause;

    public CrashReport(String description, Throwable throwable) {
        this.description = description;
        this.cause = throwable;
    }

    public String getDescription() { return this.description == null ? "" : this.description; }
    public Throwable getCrashCause() { return this.cause; }

    /**
     * Vanilla 1.5.2 factory used by Entity/EntityLiving/etc. when
     * wrapping a Throwable into a reportable crash. Returns a new
     * CrashReport with the given description and cause.
     */
    public static CrashReport makeCrashReport(Throwable throwable, String description) {
        return new CrashReport(description, throwable);
    }

    /** Vanilla also provides a method for adding a category section. */
    public CrashReportCategory makeCategory(String name) {
        return new CrashReportCategory();
    }
}
