package com.seminario2.fede.calvo.infoclima.utils;

import android.content.Context;
import android.text.format.DateUtils;

import com.seminario2.fede.calvo.infoclima.R;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Clase para convertir distintos patrones de fechas
 */
public final class InfoClimaDateUtils {

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;


    public static long getDayNumber(long date) {
        TimeZone tz = TimeZone.getDefault();
        long gmtOffset = tz.getOffset(date);
        return (date + gmtOffset) / DAY_IN_MILLIS;
    }

    //normalizamos la fecha para ingresarla a la DB, hacer la query de select, entre otras.
    //Convertimos la fecha para quitarles la zona horaria y retornar siempre 12:00AM
    public static long normalizeDate(long date) {
        long daysSinceEpoch =TimeUnit.MILLISECONDS.toDays(date);
        long millisFromEpochToTodayAtMidnightUtc = daysSinceEpoch * DAY_IN_MILLIS;
        return millisFromEpochToTodayAtMidnightUtc;
    }


    public static long getLocalDateFromUTC(long utcDate) {
        TimeZone tz = TimeZone.getDefault();
        long gmtOffset = tz.getOffset(utcDate);
        return utcDate - gmtOffset;
    }


    /*
    Clase para formatear la fecha y retornarla de un modo agradable. Convierte de milisegundos a texto
    Ejemplo fecha para hoy: "Hoy, 25 de noviembre"
    Mañana: "Mañana"
    Para los proximos 5 dias: "Miercoles"
    A partir del dia 6: "Lunes, 03 de Diciembre"
     */
    public static String getFriendlyDateString(Context context, long dateInMillis, boolean showFullDate) {

        long localDate = getLocalDateFromUTC(dateInMillis);
        long dayNumber = getDayNumber(localDate);
        long currentDayNumber = getDayNumber(System.currentTimeMillis());

        //si la fecha es hoy se mostrará: "Hoy, 25 de noviembre"
        if (dayNumber == currentDayNumber || showFullDate) {

            String dayName = getDayName(context, localDate);
            String readableDate = getReadableDateString(context, localDate);
            if (dayNumber - currentDayNumber < 2) {

                //traemos la fecha y le reemplazamos el año por el dia por ejemplo "Hoy", "Mañana", etc
                String localizedDayName = new SimpleDateFormat("EEEE").format(localDate);
                return readableDate.replace(localizedDayName, dayName);
            } else {
                return readableDate;
            }
        } else if (dayNumber < currentDayNumber + 7) {

            //si es mas de 6 dias retornamos el numero de dia y fecha completa por ejemplo: "Lunes, 03 de Diciembre"
            return getDayName(context, localDate);
        } else {
            int flags = DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_NO_YEAR
                    | DateUtils.FORMAT_ABBREV_ALL
                    | DateUtils.FORMAT_SHOW_WEEKDAY;

            return DateUtils.formatDateTime(context, localDate, flags);
        }
    }

    //chequeamos si la fecha esta normalizada para poder ingresarla a la DB.
    // el formato tiene que ser Unix time y con hora 12:00AM
    public static boolean isDateNormalized(long millisSinceEpoch) {
        boolean isDateNormalized = false;
        if (millisSinceEpoch % DAY_IN_MILLIS == 0) {
            isDateNormalized = true;
        }

        return isDateNormalized;
    }

    //muestra la fecha como String sin el año
    private static String getReadableDateString(Context context, long timeInMillis) {
        int flags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY;

        return DateUtils.formatDateTime(context, timeInMillis, flags);
    }

    /*
    Retornamos el nombre del dia. "Hoy", "Mañana", "Viernes", etc. dependiendo del idioma en el dispositivo
     */
    private static String getDayName(Context context, long dateInMillis) {

        long dayNumber = getDayNumber(dateInMillis);
        long currentDayNumber = getDayNumber(System.currentTimeMillis());
        if (dayNumber == currentDayNumber) {
            /*
            Retorno "Hoy", "Today" o la traduccion que encuentre en el archivo de String values
             */
            return context.getResources().getString(R.string.today);
        } else if (dayNumber == currentDayNumber + 1) {

            /*
            Retorno "Mañana", "Tomorrow" o la traduccion que encuentre en el archivo de String values
             */
            return context.getResources().getString(R.string.tomorrow);
        } else {
            /*
            si no es hoy ni mañana, retornamos el nombre del dia, por ejemplo: "Jueves"
             */
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /*
    Retornamos la fecha de hoy normalizada, esto es, con la hora en 12:00AM en GMT time
     */
    public static long getNormalizedUtcDateForToday() {

        long utcNowMillis = System.currentTimeMillis();

        TimeZone currentTimeZone = TimeZone.getDefault();

        long gmtOffsetMillis = currentTimeZone.getOffset(utcNowMillis);

        long timeSinceEpochLocalTimeMillis = utcNowMillis + gmtOffsetMillis;

        long daysSinceEpochLocal = TimeUnit.MILLISECONDS.toDays(timeSinceEpochLocalTimeMillis);

        long normalizedUtcMidnightMillis = TimeUnit.DAYS.toMillis(daysSinceEpochLocal);

        return normalizedUtcMidnightMillis;
    }
}
