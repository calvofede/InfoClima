package com.seminario2.fede.calvo.infoclima.sync;

import android.content.ContentValues;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.format.DateUtils;
import android.util.Log;

import com.seminario2.fede.calvo.infoclima.darkSkyAPIUtils.DarkSkyAPIUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaNotificationUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaPreferences;
import com.seminario2.fede.calvo.infoclima.utils.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.seminario2.fede.calvo.infoclima.MainActivity.CONTENT_URI;

/**
 * Esta clase:
 * Hace la request a la API de clima
 * Parsea el JSON de la request y lo inserta en la tabla mediante el ContentProvider
 * Notifica al usuario del clima del dia si no ha sido notificado en mas de 1 dia y si tiene
 * las notificaciones activas en la parte de "Configuracion"
 */
public class InfoClimaSyncTask {

    synchronized public static void syncWeather(Context context) {

        try {
            String stringLocation = InfoClimaPreferences.getWeatherPreferredLocation(context);

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            List<Address> addressList = new ArrayList<>();
            try {
                addressList = geocoder.getFromLocationName(stringLocation, 3);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Si la lista no esta vacia, traemos esas coordenadas para armar la URI, usamos la
            //primera de la lista por ser el mejor matcheo a nuestra busqueda
            String locationCoords = null;
            if (!addressList.isEmpty()) {
                locationCoords = String.valueOf(
                        addressList.get(0).getLatitude()).concat(",")
                        .concat(String.valueOf(addressList.get(0).getLongitude()));

                InfoClimaPreferences.saveLastLocationFoundCorrectly(
                        context, InfoClimaPreferences.getWeatherPreferredLocation(context));

                InfoClimaPreferences.resetLocationCoordinates(context);

            } else {
                //si la lista esta vacia usamos las coordenadas que estaban anteriormente guardadas
                locationCoords = InfoClimaPreferences.getLocationCoordinates(context);
            }

            //buildeamos la query en base a las coordenadas
            URL url = NetworkUtils.buildUrl(locationCoords);

            String jsonResponse = NetworkUtils.getResponseFromHttpUrl(url);

            //parseamos la respuesta de la API
            ContentValues[] weatherValues = DarkSkyAPIUtils.getWeatherContentValuesFromJson(context, jsonResponse);

            if (weatherValues != null && weatherValues.length != 0) {

                //aca eliminamos toda data vieja de la tabla
                context.getContentResolver().delete(CONTENT_URI, null, null);

                //insertamos la nueva data del clima en la tabla
                context.getContentResolver().bulkInsert(CONTENT_URI, weatherValues);

                //chequeamos si las notificaciones estan activas
                boolean notificationsEnabled = InfoClimaPreferences.areNotificationsEnabled(context);

                long timeSinceLastNotification = InfoClimaPreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean fourHoursPassedSinceLastNotification = false;

                if (timeSinceLastNotification >= TimeUnit.HOURS.toMillis(4)) {
                    fourHoursPassedSinceLastNotification = true;
                }

                /*
                Chequeamos que las notificaciones esten activas y haya pasado mas de 4 horas desde
                la ultima vez que se mostro una notificacion
                 */
                if (notificationsEnabled && fourHoursPassedSinceLastNotification) {
                    InfoClimaNotificationUtils.notifyUserOfNewWeather(context);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
