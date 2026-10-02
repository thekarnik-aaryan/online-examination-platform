package exception;

/** Base class for errors whose message is safe to show to the end user. */
public class AppException extends RuntimeException {
    public AppException(String userMessage) { super(userMessage); }
    public AppException(String userMessage, Throwable cause) { super(userMessage, cause); }
}
