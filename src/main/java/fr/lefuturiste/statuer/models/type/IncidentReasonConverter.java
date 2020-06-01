package fr.lefuturiste.statuer.models.type;

import fr.lefuturiste.statuer.models.type.IncidentReason;
import org.json.JSONObject;

import javax.persistence.AttributeConverter;

public class IncidentReasonConverter implements AttributeConverter<IncidentReason, String> {

  @Override
  public String convertToDatabaseColumn(IncidentReason reason) {
    if (reason == null) {
      return null;
    }
    JSONObject object = new JSONObject()
      .put("code", reason.getCode())
      .put("message", reason.getMessage());

    if (reason != null) {
      object.put("debug", reason.getDebug());
    }

     return object.toString();
  }

  @Override
  public IncidentReason convertToEntityAttribute(String reasonJSON) {
    if (reasonJSON == null || reasonJSON.length() == 0) {
      return null;
    }
    JSONObject jsonParsed = new JSONObject(reasonJSON);
    return new IncidentReason(
      jsonParsed.getString("code"),
      jsonParsed.getString("message"),
      jsonParsed.has("debug") ? jsonParsed.getString("debug") : null
    );
  }

}