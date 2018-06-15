package com.savonia.thesis.webclient.measuremetsmodels;

import android.arch.lifecycle.LiveData;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
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

    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @GET("json/measurements/{key}?")
    Call<List<MeasurementsModel>> getMeasurements(@Path("key") String key, @Query("obj") String measName,
        @Query("tag") String measTag, @Query("from") String fromDate, @Query("to") String toDate, @Query("take") Integer take, @Query("data-tags") String dataTags);

    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @GET("json/measurements/{key}?")
    Call<ResponseBody> getMeasurementsAsString(@Path("key") String key, @Query("obj") String measName,
        @Query("tag") String measTag, @Query("from") String fromDate, @Query("to") String toDate, @Query("take") Integer take, @Query("data-tags") String dataTags);


    @Headers({
            "User-Agent: Fiddler",
            "Host: sami.savonia.fi",
            "Content-Type: text/json"
    })
    @POST("json/measurements/save")
    Call<ResponseBody> postMeasurements(@Body MeasurementsPackage measurementsPackage);
}
