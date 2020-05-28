package fr.lefuturiste.statuer.models.type;

public class IncidentReason {

    private String code;

    private String message;

    public IncidentReason() {}

    /**
     * Create a incident reason
     *
     * @param code The slugged code
     * @param message A free message to describe exactly what caused the issue
     */
    public IncidentReason(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return "IncidentReason{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public IncidentReason setCode(String code) {
        this.code = code;
        return this;
    }

    public IncidentReason setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEmpty() {
        return message == null || code == null || message.length() == 0 && code.length() == 0;
    }
}
