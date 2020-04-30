package fr.lefuturiste.statuer.checker;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.models.Service;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class HttpChecker implements CheckerInterface {

    private static OkHttpClient httpClient;

    public HttpChecker() {
        httpClient = new OkHttpClient();
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
        try {
            response = httpClient.newCall(request).execute();
            response.close();
            code = String.valueOf(response.code());
        } catch (IOException err) {
            App.logger.debug("Request failed with url " + service.getUrl() + " of service: " + service.getPath());
            App.logger.debug(err.toString());
        }
        App.logger.debug("Got http code '" + code + "' for service " + service.getPath() + " at url " + service.getUrl());
        return !code.equals("") && code.charAt(0) == '2';
    }
}
