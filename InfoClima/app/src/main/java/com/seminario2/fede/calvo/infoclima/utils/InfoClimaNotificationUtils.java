package com.seminario2.fede.calvo.infoclima.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.seminario2.fede.calvo.infoclima.R;
import com.seminario2.fede.calvo.infoclima.WeatherDetailActivity;
import com.seminario2.fede.calvo.infoclima.darkSkyAPIUtils.DarkSkyAPIUtils;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.seminario2.fede.calvo.infoclima.MainActivity.CONTENT_URI;
import static com.seminario2.fede.calvo.infoclima.MainActivity.INTENT_EXTRA_TO_QUERY_W_DETAILS;

public class InfoClimaNotificationUtils {

    protected static String[] CONTENT_PROVIDER_COLUMN_NAMES = {
            "date",
            "weather_id",
            "min",
            "max",
            "humidity",
            "pressure",
            "wind",
            "degrees",
            "summary"
    };

    protected static int DATE_COLUMN_INDEX = 0;
    protected static int WEATHER_ID_COLUMN_INDEX = 1;
    protected static int MIN_COLUMN_INDEX = 2;
    protected static int MAX_COLUMN_INDEX = 3;
    protected static int HUMIDITY_COLUMN_INDEX = 4;
    protected static int PRESSURE_COLUMN_INDEX = 5;
    protected static int WIND_COLUMN_INDEX = 6;
    protected static int DEGREES_COLUMN_INDEX = 7;
    protected static int SUMMARY_COLUMN_INDEX = 8;

    //ID del canal de notificaciones que es obligatorio de usar si el dispositivo tiene Android
    // con API igual o mayor a 26
    private static final String NOTIFICATION_CHANNEL = "14";

    //ID para acceder a la notificacion una vez que la mostramos
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    /*
    Notificamos al usuario con un breve resumen del clima del DIA en curso
     */
    public static void notifyUserOfNewWeather(Context context) {

        Uri uriQuery = CONTENT_URI.buildUpon()
                .appendPath(Long.toString(InfoClimaDateUtils.normalizeDate(System.currentTimeMillis())))
                .build();

        //URI para realizar la consulta y el String array con las columnas a traer
        Cursor todayWeatherCursor = context.getContentResolver().query(
                uriQuery,
                CONTENT_PROVIDER_COLUMN_NAMES,
                null,
                null,
                null);

        //movemos el cursor a la primera ubicacion
        // Si es FALSE cerramos el cursor y no se mostrara la notificacion
        if (todayWeatherCursor.moveToFirst()) {

            String weatherDescription = todayWeatherCursor.getString(SUMMARY_COLUMN_INDEX);
            double high = todayWeatherCursor.getDouble(MAX_COLUMN_INDEX);
            double low = todayWeatherCursor.getDouble(MIN_COLUMN_INDEX);

            String weatherSummary = todayWeatherCursor.getString(WEATHER_ID_COLUMN_INDEX);

            //metodo para traer el icono a mostrar dependiendo de la palabra clave que nos retorna la
            //API, por ejemplo RAIN, CLOUDY, etc
            int largeArtResourceId = DarkSkyAPIUtils.getArtResourceForWeatherCondition(weatherSummary);

            //Titulo de la notificacion
            String notificationTitle = context.getString(R.string.weather_notification_prefix);

            String notificationText = getNotificationText(context, high, low, weatherDescription);

            // Chequeamos si la version de Android del dispositivo es mayor a 26, si es asi
            //debemos crear un NOTIFICATION CHANNEl, de lo contrario no se mostrara la notificacion
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Notificación del clima de día";
                String description = "InfoClima notificaciones";
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
                channel.setDescription(description);
                // Registamos el canal en el sistema. Despues de esto no podemos cambiar la importancia del canal
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }


            //Preparamos la notificacion, le seteamos el color, el icono, el titulo, el contenido.
            //El AutoCancel true hace que al clickear en la notificacion ésta desaparezca
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setSmallIcon(largeArtResourceId)
                    .setLargeIcon(null)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setAutoCancel(true);

            //Chequeamos si la version de Android del dispositivo es mayor o igual a la 26
            //y si es asi seteamos el channelId
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(NOTIFICATION_CHANNEL);
            }

            //Creamos un intent para enviar a WeatherDetailActivity. El usuario al clickear en la
            //notificacion, esta redirigira hacia el activity de Details
            Intent detailIntentForToday = new Intent(context, WeatherDetailActivity.class);
            detailIntentForToday.putExtra(INTENT_EXTRA_TO_QUERY_W_DETAILS, Long.valueOf(uriQuery.getLastPathSegment()));


            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addNextIntentWithParentStack(detailIntentForToday);
            PendingIntent resultPendingIntent = taskStackBuilder
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            notificationBuilder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(NOTIFICATION_SERVICE);

            //llamando a este metodo se muestra la notificacion.
            notificationManager.notify(WEATHER_NOTIFICATION_ID, notificationBuilder.build());

            //guardamos el tiempo en el cual se mostro la notificacion
            InfoClimaPreferences.saveLastNotificationTime(context, System.currentTimeMillis());
        }

        todayWeatherCursor.close();
    }

    /*
    Creamos el body de la notificacion
     */
    private static String getNotificationText(Context context
            , double high, double low, String weatherDesc) {

        String notificationText = weatherDesc +
                " " +
                "Max: " + DarkSkyAPIUtils.formatTemperature(context, high) +
                " " +
                "Min: " + DarkSkyAPIUtils.formatTemperature(context, low);

        return notificationText;
    }
}
