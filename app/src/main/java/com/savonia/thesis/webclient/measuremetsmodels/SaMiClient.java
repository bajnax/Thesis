package com.savonia.thesis.webclient.measuremetsmodels;

import android.arch.lifecycle.LiveData;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SaMiClient {
    String key = "savoniatest";

    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @GET("json/measurements/" + key)
    Call<List<MeasurementsModel>> getMeasurements();


    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })

    @POST("json/measurements/save")
    @FormUrlEncoded
    Call<MeasurementsModel> postMeasurements(@Field("key") String key, @Body MeasurementsModel measurementsModel);
}
