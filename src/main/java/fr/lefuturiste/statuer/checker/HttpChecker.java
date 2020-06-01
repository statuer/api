package fr.lefuturiste.statuer.checker;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.models.type.IncidentReason;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class HttpChecker implements CheckerInterface {

  private static OkHttpClient httpClient;

  private IncidentReason reason = null;

  public HttpChecker(int timeout) {
    httpClient = new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS).writeTimeout(timeout, TimeUnit.SECONDS)
        .callTimeout(timeout, TimeUnit.SECONDS).build();
  }

  /**
   * Will consider a http service as UP only if the status code start with a 2
   * (2XX)
   *
   * @param service The service to check
   * @return boolean
   */
  public boolean isAvailable(Service service) {
    Request request = new Request.Builder().url(service.getUrl()).build();
    Response response = null;
    String code = "";
    String body = "";
    boolean success = true;
    IncidentReason tmpReason = new IncidentReason();
    try {
      response = httpClient.newCall(request).execute();
      body = response.body().string();
      response.close();
      code = String.valueOf(response.code());
    } catch (IOException err) {
      success = false;
      App.logger.debug("    Request failed with url " + service.getUrl() + " of service: " + service.getPath());
      App.logger.debug("    " + err.toString());
      tmpReason.setCode("http-checker-error");
      tmpReason.setMessage(err.toString());
    }
    if (success) {
      App.logger
          .debug("    Got http code '" + code + "' for service " + service.getPath() + " at url " + service.getUrl());
    }
    // (code.equals("") || code.charAt(0) != '2')
    if (tmpReason.isEmpty() && response != null && !response.isSuccessful()) {
      success = false;
      String message = "Invalid HTTP code, got " + code + " wanted 2XX";
      App.logger.debug("    " + message);
      tmpReason.setCode("http-invalid-status-code");
      tmpReason.setMessage(message);
      JSONObject debug = new JSONObject()
        .put("status", response.message()).put("headers", response.headers().toMultimap()).put("body", body);
      App.logger.debug(debug.toString());
      tmpReason.setDebug(debug.toString());
    }
    if (!success) {
      reason = tmpReason;
    }
    return success;
  }

  public IncidentReason getReason() {
    return reason;
  }
}
