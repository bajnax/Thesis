package com.savonia.thesis.webclient;

import android.util.Log;

import com.savonia.thesis.webclient.measuremetsmodels.SaMiClient;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebClient {
    private static final String TAG = WebClient.class.getSimpleName();
    private static final String saMiUrl = "https://sami.savonia.fi/Service/3.0/MeasurementsService.svc/";
    private static Retrofit retrofit = null;
    private static SaMiClient saMiClient = null;

    public static SaMiClient getWebClient() {
        if(saMiClient == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            retrofit = new Retrofit.Builder()
                    .baseUrl(saMiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
            saMiClient =  retrofit.create(SaMiClient.class);
        }
        return saMiClient;
    }
}
