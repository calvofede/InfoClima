package com.seminario2.fede.calvo.infoclima.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.seminario2.fede.calvo.infoclima.R;

/**
 * Clase para retornar las preferencias del usuario, que estan seleccionadas en el activity Settings
 * Luego se usaran estos metodos para actualizar la UI en la MainActivity segun corresponda
 */
public class InfoClimaPreferences {

    private static final String LOCATION_PREF_KEY = "location";
    private static final String TEMPERATURE_UNITS_PREF_KEY = "units";
    private static final String LAST_NOTIFICATION_KEY = "last_notification";

    public static String getWeatherPreferredLocation(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String locationValue = preferences.getString(LOCATION_PREF_KEY, context.getString(R.string.default_location));

        return locationValue;
    }

    public static void saveLastLocationFoundCorrectly(Context context, String location) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("lastSavedLocation", location);
        editor.apply();
    }

    public static String getLastLocationFoundCorrectly(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("lastSavedLocation", "def");
    }

    //Chequeamos si el usuario selecciono el sistema metrico para formatear los valores mostrados en consecuencia
    public static boolean isMetric(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String unitsValue = preferences.getString(TEMPERATURE_UNITS_PREF_KEY, context.getString(R.string.pref_metric_value));

        return unitsValue.equals(context.getString(R.string.pref_metric_value));
    }

    //seteamos las coordenadas de la location seleccionada. Utilizado para abrir el mapa con la ubicacion
    // el cual es un item del menu en MainActivity
    public static void setLocationDetails(Context context, double lat, double lon) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.putLong("coord_lat", Double.doubleToRawLongBits(lat));
        editor.putLong("coord_long", Double.doubleToRawLongBits(lon));
        editor.apply();
    }

    //reseteamos las coordenadas, luego de cambiar la ubicacion por ejemplo
    public static void resetLocationCoordinates(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.remove("coord_lat");
        editor.remove("coord_long");
        editor.apply();
    }

    //traemos las coordenadas, para por ejemplo abrir el mapa
    public static String getLocationCoordinates(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

        Double latitude = Double
                .longBitsToDouble(sp.getLong("coord_lat", Double.doubleToRawLongBits(0.0)));
        Double longitude = Double
                .longBitsToDouble(sp.getLong("coord_long", Double.doubleToRawLongBits(0.0)));

        String latAndLongToQuery = latitude.toString().concat(",").concat(longitude.toString());

        return latAndLongToQuery;
    }

    /*
    Chequeamos si las notificaciones estan activas o no
     */
    public static boolean areNotificationsEnabled(Context context) {
        //Key para chequear las notificaciones activas o no
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);

        //booleano para mostrar notificaciones por default, por ejemplo al recien instalar la APP
        boolean shouldDisplayNotificationsByDefault = context
                .getResources()
                .getBoolean(R.bool.show_notifications_by_default);

        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

        //si logramos recuperar la setting de si estan activas o no las notificaciones retornamos eso
        //sino el valor por default
        return sp.getBoolean(displayNotificationsKey, shouldDisplayNotificationsByDefault);
    }

    /*
    retorna el tiempo en milisegundos de cuando se realizo la ultima notificacion, utilizado para
    llevar un registro del tiempo y no spamear al usuario con notificaciones
     */
    public static long getLastNotificationTimeInMillis(Context context) {

        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);

        //traemos los milisegundos de la ultima notificacion mostrada. De lo contario, CERO si no
        //se puede recuperar el valor. Retornamos CERO ya que cuando se quiere mostrar una notificacion
        //se resta el tiempo en ese momento con este tiempo que retornamos  en este metodo y si es
        //mayor a un dia, mostramos otra notificacion. Al retornar 0 siempre sera mayor a 1 dia, y mostramos
        //una nueva notificacion
        return sp.getLong(LAST_NOTIFICATION_KEY, 0);
    }

    /*
    Se comprueba el tiempo que pasó desde la ultima notificacion y los milisegundos del momento que
    se llama a este metodo
     */
    public static long getEllapsedTimeSinceLastNotification(Context context) {
        long lastNotificationTimeMillis =
                InfoClimaPreferences.getLastNotificationTimeInMillis(context);
        return System.currentTimeMillis() - lastNotificationTimeMillis;
    }

    /*
    Se guarda el tiempo en milisegundos de cuando se realizo la notificacion. Para tener registro
    y no enviar mas de 1 notificacion al dia al usuario
     */
    public static void saveLastNotificationTime(Context context, long timeOfNotification) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(LAST_NOTIFICATION_KEY, timeOfNotification);
        editor.apply();
    }

    public static void saveLanguage(Context context, String language) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("language", language);
        editor.apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("language", "def");
    }

}
