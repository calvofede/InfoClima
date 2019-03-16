package com.seminario2.fede.calvo.infoclima.darkSkyAPIUtils;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.seminario2.fede.calvo.infoclima.R;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaDateUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * Clase Utils creada para la API Dark Sky para convertir temperaturas, formatear las unidades de viento,
 * mapear mensajes en relacion a los iconos y descripcion del clima diario, entre otros.
 */
public class DarkSkyAPIUtils {

    private static final String LOG_TAG = DarkSkyAPIUtils.class.getSimpleName();

    /*
    Formateamos la temperatura dependiendo de las settings del usuario
     */
    public static String formatTemperature(Context context, double temperature) {
        int temperatureFormatResourceId = R.string.format_temperature_celsius;

        if (!InfoClimaPreferences.isMetric(context)) {
            temperature = celsiusToFahrenheit(temperature);
            temperatureFormatResourceId = R.string.format_temperature_fahrenheit;
        }

        return String.format(context.getString(temperatureFormatResourceId), temperature);
    }

    /*
    Metodo similar al de formatear la temperatura, formateamos la velocidad del viento dependiendo
    si el usuario seleccionó sistema Metrico o Imperial
     */
    public static String getFormattedWind(Context context, float windSpeed, float degrees) {

        int windFormat = R.string.format_wind_kmh;

        if (!InfoClimaPreferences.isMetric(context)) {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        //Aqui seleccionamos la direccion del viento
        String direction = "Desconocido";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }
        //usamos el metodo String.format para cargando el resource de windFormat
        //segun la preferencia del usuario, retornar el string relacionado al wind con la
        //direccion del viento y unidad de medida de la velocidad del mismo
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    private static double celsiusToFahrenheit(double temperatureInCelsius) {
        double temperatureInFahrenheit = (temperatureInCelsius * 1.8) + 32;
        return temperatureInFahrenheit;
    }

    /*
    Aqui retornamos distintos iconos del clima dependiendo de la palabra clave de ese dia que nos llegue
     */
    public static int getArtResourceForWeatherCondition(String weather) {
        /*
         * Strings de weather se pueden encontrar aqui en la documentacion de la API
         * https://darksky.net/dev/docs#data-point (icon)
         */
        if (weather.equalsIgnoreCase("clear-day")) {
            return R.drawable.art_clear;
        } else if (weather.equalsIgnoreCase("clear-night")) {
            return R.drawable.art_clear;
        } else if (weather.equalsIgnoreCase("rain")) {
            return R.drawable.art_rain;
        } else if (weather.equalsIgnoreCase("snow")) {
            return R.drawable.art_snow;
        } else if (weather.equalsIgnoreCase("sleet")) {
            return R.drawable.art_snow;
        } else if (weather.equalsIgnoreCase("wind")) {
            return R.drawable.art_fog;
        } else if (weather.equalsIgnoreCase("fog")) {
            return R.drawable.art_fog;
        } else if (weather.equalsIgnoreCase("cloudy")) {
            return R.drawable.art_clouds;
        } else if (weather.equalsIgnoreCase("partly-cloudy-day")) {
            return R.drawable.art_light_clouds;
        } else if (weather.equalsIgnoreCase("partly-cloudy-night")) {
            return R.drawable.art_clouds;
        } else if (weather.equalsIgnoreCase("thunderstorm")) {
            return R.drawable.art_storm;
        } else if (weather.equalsIgnoreCase("tornado")) {
            return R.drawable.art_storm;
        } else if (weather.equalsIgnoreCase("hail")) {
            return R.drawable.art_storm;
        }
        Log.e(LOG_TAG, "Unknown Weather: " + weather);
        return R.drawable.art_clouds;
    }


    /*
    Aqui retornamos la respuesta de la API y parseamos el String para ponerlo en un array de ContentValues
    y posteriormente poder insertarlo a la tabla para consultarlos sin conexion
     */
    public static ContentValues[] getWeatherContentValuesFromJson(Context context, String forecastJsonStr)
            throws JSONException {

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        double cityLongitude = forecastJson.getDouble("longitude");
        double cityLatitude = forecastJson.getDouble("latitude");
        //guardamos las coordenadas de la ubicacion seleccionada, la cual podria utilizarse
        //para volver a mostrar esa location si el servicio no encuentra la nueva direccion cargada
        //en la pantalla de Configuración
        InfoClimaPreferences.setLocationDetails(context, cityLatitude, cityLongitude);

        JSONObject dailyJson = forecastJson.getJSONObject("daily");

        JSONArray jsonWeatherArray = dailyJson.getJSONArray("data");

        String weekWeatherDesc = forecastJson.getJSONObject("daily").getString("summary");
        String weekIcon = forecastJson.getJSONObject("daily").getString("icon");

        ContentValues[] weatherContentValues = new ContentValues[jsonWeatherArray.length()];

        long normalizedUtcStartDay = InfoClimaDateUtils.getNormalizedUtcDateForToday();

        for (int i = 0; i < jsonWeatherArray.length(); i++) {

            double pressure;
            double humidity;
            double windSpeed;
            double windDirection;

            double maxTemp;
            String summary;
            double minTemp;
            long dateTimeMillis;

            dateTimeMillis = normalizedUtcStartDay + InfoClimaDateUtils.DAY_IN_MILLIS * i;

            JSONObject dayForecast = jsonWeatherArray.getJSONObject(i);

            pressure = dayForecast.getDouble("pressure");
            humidity = dayForecast.getDouble("humidity");

            /*
            Guardamos el timestamp para mostrar el valor en el MainActivity de la ultima vez actualizado
             */
            long lastUpdate = System.currentTimeMillis();


            /*
            Formateamos la humedad y le agregamos 2 ceros ya que nos llega por ejemplo, 0.61 en vez de 61
               */
            String percentageHumidity = BigDecimal.valueOf(humidity).multiply(
                    BigDecimal.valueOf(100)).setScale(0).toString();

            windSpeed = dayForecast.getDouble("windSpeed");
            windDirection = dayForecast.getDouble("windBearing");

            String weatherIcon = dayForecast.getString("icon");

            maxTemp = dayForecast.getDouble("temperatureHigh");
            minTemp = dayForecast.getDouble("temperatureLow");
            summary = dayForecast.getString("summary");

            //Guardamos una lista de ContentValues que luego usaremos para insertarlas en la DB
            ContentValues weatherValues = new ContentValues();
            weatherValues.put("date", dateTimeMillis);
            weatherValues.put("weekSummary", weekWeatherDesc);
            weatherValues.put("humidity", percentageHumidity);
            weatherValues.put("pressure", pressure);
            weatherValues.put("wind", windSpeed);
            weatherValues.put("degrees", windDirection);
            weatherValues.put("max", maxTemp);
            weatherValues.put("min", minTemp);
            weatherValues.put("weather_id", weatherIcon);
            weatherValues.put("summary", summary);
            weatherValues.put("weekIcon", weekIcon);
            weatherValues.put("lastUpdate", lastUpdate);

            weatherContentValues[i] = weatherValues;
        }

        return weatherContentValues;
    }
}
