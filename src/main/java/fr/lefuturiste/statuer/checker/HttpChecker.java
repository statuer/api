package fr.lefuturiste.statuer.checker;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.models.type.IncidentReason;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpChecker implements CheckerInterface {

    private static OkHttpClient httpClient;

    private IncidentReason reason = null;

    public HttpChecker() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Will consider a http service as UP only if the status code start with a 2 (2XX)
     *
     * @param service The service to check
     * @return boolean
     */
    public boolean isAvailable(Service service) {
        Request request = new Request.Builder().url(service.getUrl()).build();
        Response response;
        String code = "";
        boolean success = true;
        IncidentReason tmpReason = new IncidentReason();
        try {
            response = httpClient.newCall(request).execute();
            response.close();
            code = String.valueOf(response.code());
        } catch (IOException err) {
            success = false;
            App.logger.debug("Request failed with url " + service.getUrl() + " of service: " + service.getPath());
            App.logger.debug(err.toString());
            tmpReason.setCode("http-checker-error");
            tmpReason.setMessage(err.toString());
        }
        App.logger.debug("Got http code '" + code + "' for service " + service.getPath() + " at url " + service.getUrl());
        if ((code.equals("") || code.charAt(0) != '2') && tmpReason.isEmpty()) {
            success = false;
            String message = "Invalid HTTP code, got " + code + " wanted 2XX";
            App.logger.debug(message);
            tmpReason.setCode("http-invalid-status-code");
            tmpReason.setMessage(message);
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
