package fr.lefuturiste.statuer.models.type;

import fr.lefuturiste.statuer.models.type.IncidentReason;
import org.json.JSONObject;

import javax.persistence.AttributeConverter;

public class IncidentReasonConverter implements AttributeConverter<IncidentReason, String> {

    @Override
    public String convertToDatabaseColumn(IncidentReason reason) {
        return new JSONObject()
                .put("code", reason.getCode())
                .put("message", reason.getMessage())
                .toString();
    }

    @Override
    public IncidentReason convertToEntityAttribute(String reasonJSON) {
        if (reasonJSON == null || reasonJSON.length() == 0) {
            return null;
        }
        JSONObject jsonParsed = new JSONObject(reasonJSON);
        return new IncidentReason(
                jsonParsed.getString("code"),
                jsonParsed.getString("message")
        );
    }

}