package salt.samczsun.exception;

/**
 * Represents a {@link SkypeException SkypeException} which holds another exception as the cause
 *
 * @author samczsun
 */
@SuppressWarnings("serial")
public class NestedSkypeException extends SkypeException {
    private final Exception reason;

    public NestedSkypeException(String cause, Exception suppressed) {
        super(cause);
        this.addSuppressed(suppressed);
        this.reason = suppressed;
    }

    public Exception getReason() {
        return this.reason;
    }
}
