package com.savonia.thesis.webclient.measuremetsmodels;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface SaMiClient {
    String key = "savoniatest";

    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @GET("/json/measurements/" + key)
    Call<List<MeasurementsModel>> measurements(
    );
}
