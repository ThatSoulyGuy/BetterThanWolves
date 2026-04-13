package btw.modern;

/**
 * Vanilla 1.5.2 wrapping exception for fatal crashes that include a
 * {@link CrashReport}. Used by Entity / EntityLiving / DataWatcher /
 * ChunkProviderServer / etc. when wrapping a Throwable into a
 * crash-reportable form.
 *
 * In our bridge we don't run vanilla's full crash-report pipeline, so
 * this is a thin wrapper that exposes a CrashReport accessor.
 */
public class ReportedException extends RuntimeException {
    private final CrashReport theReportedExceptionCrashReport;

    public ReportedException(CrashReport report) {
        this.theReportedExceptionCrashReport = report;
    }

    public CrashReport getCrashReport() {
        return this.theReportedExceptionCrashReport;
    }

    @Override
    public String getMessage() {
        return this.theReportedExceptionCrashReport == null
                ? "ReportedException" : this.theReportedExceptionCrashReport.getDescription();
    }
}
