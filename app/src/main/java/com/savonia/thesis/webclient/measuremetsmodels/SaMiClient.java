package com.savonia.thesis.webclient.measuremetsmodels;

import android.arch.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SaMiClient {
    String key = "savoniatest";

    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @GET("json/measurements/") // TODO: check if problem is in the key (not query)
    Call<List<MeasurementsModel>> getMeasurements(@Query("key") String key, @Query("obj") String measName,
    @Query("tag") String measTag, @Query("take") Integer take, @Query("data-tags") String dataTags);


    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @POST("json/measurements/save")
    Call<String> postMeasurements(@Body MeasurementsPackage measurementsPackage);
}
