package com.seminario2.fede.calvo.infoclima.utils;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Scanner;

/**
 * Clase para realizar los llamados a la API del clima.
 *
 * Dark Sky
 * https://darksky.net/dev/docs
 */
public class NetworkUtils {

    private static final String WEATHER_API_BASE_URL = "https://api.darksky.net/";
    private static final String API_KEY = "07b356e63244178dcf15052735d38cb0";
    private static final String FORECAST = "forecast";
    private static final String LANGUAGE = "lang";
    private static final String UNITS = "units";

    public static URL buildUrl(String location) {

        String language = Locale.getDefault().getLanguage();

        //Realizamos la llamada HTTP a la API pasandole el Language del dispositivo
        //Excluimos de la llamada algunos datos que no utilizamos para de esta forma acelerar la respuesta
        //Usamos las units en CA ya que de esta forma muestra las unidades en el sistema internacional
        //y la velocidad del viento en kph
        Uri uri = Uri.parse(WEATHER_API_BASE_URL).buildUpon()
                .appendPath(FORECAST)
                .appendPath(API_KEY)
                .appendPath(location)
                .appendQueryParameter(LANGUAGE, language)
                .appendQueryParameter("exclude", "flags,currently,minutely,hourly,alerts")
                .appendQueryParameter(UNITS, "ca")
                .build();

        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    //Recuperamos la respuesta retornando el JSON como string que luego parsearemos
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                Log.e("NETWORK_UTILS_TAG","ERROR retrieving weather data from Internet");
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }


}
