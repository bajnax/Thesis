package com.savonia.thesis.webclient;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebClient {
    private static final String saMiUrl = "https://sami.savonia.fi/Service/3.0/MeasurementsService.svc";
    private static Retrofit retrofit = null;

    public static Retrofit getWebClient() {
        if(retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            retrofit = new Retrofit.Builder()
                    .baseUrl(saMiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
            //saMiClient =  retrofit.create(SaMiClient.class);
        }
        return retrofit;
    }
}
