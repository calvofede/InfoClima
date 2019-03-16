package com.seminario2.fede.calvo.infoclima.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Esta clase es la que nos provee de datos y aqui realizamos las tareas de
 * bulkInsert, query, delete.
 */
public class InfoClimaProvider extends ContentProvider {

    private static final String COLUMN_DATE = "date";
    private static final String CONTENT_AUTHORITY = "com.seminario2.fede.calvo.infoclima";
    private InfoClimaDatabaseHelper infoClimaDatabaseHelper;

    //Constantes utilizadas para matchear URIs
    public static final int CODE_WEATHER = 100;
    public static final int CODE_WEATHER_WITH_DATE = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    @Override
    public boolean onCreate() {
        infoClimaDatabaseHelper = new InfoClimaDatabaseHelper(getContext());
        return true;
    }

    /*
    Metodo para crear patrones de URI para luego usar en los metodos de QUERY, INSERT, etc
    De esta forma no tenemos que utilizar patrones con expresiones regulares. Lo que puede llegar a causar
    bugs
     */
    public static UriMatcher buildUriMatcher() {


        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CONTENT_AUTHORITY;

        /*
         * Para cada tipo de URI creamos un codigo previamente (CODE_WEATHER Y CODE_WEATHER_WITH_DATE).
         *
         * Esta URI es content://com.seminario2.fede.calvo.infoclima/weather/
         */
        matcher.addURI(authority, "weather", CODE_WEATHER);

        /*
         * Ejemplo de esta URI content://com.seminario2.fede.calvo.infoclima/weather/1472214172
         * El "/#" significa que si PATH_WEATHER es seguido de cualquier numero, tiene que
         * retornar el codigo CODE_WEATHER_WITH_DATE
         */
        matcher.addURI(authority, "weather" + "/#", CODE_WEATHER_WITH_DATE);

        return matcher;
    }

    /*
    Metodo utilizado para realizar un insert uno tras otro.
    Se utiliza luego de descargar los nuevos datos del clima de la API, los almacenamos para que esten
    disponibles sin conexion, y para no realizar tantas llamadas a la API sobrecargando los recursos
    de red del dispositivo
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = infoClimaDatabaseHelper.getWritableDatabase();

        //Chequeamos si matchea la URI que nos llega
        switch (sUriMatcher.match(uri)) {

            case CODE_WEATHER:
                db.beginTransaction();
                int rowsInserted = 0;
                try {
                    for (ContentValues value : values) {
                        Long weatherDate =
                                value.getAsLong(COLUMN_DATE);
                        if (!InfoClimaDateUtils.isDateNormalized(weatherDate)) {
                            throw new IllegalArgumentException("Date must be normalized to insert");
                        }

                        long _id = db.insert("weather", null, value);
                        if (_id != -1) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return rowsInserted;

            default:
                return super.bulkInsert(uri, values);
        }
    }

    /*
    Maneja las request de los clientes. Utilizamos este metodo para traer la informacion del clima
    para toda la semana (MainActivity) como asi tambien de un dia en particular (WeatherDetailActivity)
    Projection es la lista de columnas a insertar en el cursor
    Selection es el criterio para filtrar rows.
    SelectionArgs son los parametros que se usaran en el criterio de busqueda Selection
    SortOrder para darle un orden a la busqueda
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor;

        switch (sUriMatcher.match(uri)) {

            case CODE_WEATHER_WITH_DATE: {

                //tomamos el ultimo path segment que corresponde a la fecha normalizada en milisegundos
                String normalizedUtcDateString = uri.getLastPathSegment();

                String[] selectionArguments = new String[]{normalizedUtcDateString};

                cursor = infoClimaDatabaseHelper.getReadableDatabase().query(
                        //nombre de la tabla
                        "weather",
                        //columnas que queremos que retorne la query
                        projection,
                        //se reemplaza el ? por la fecha normalizada en milisegundos (selectionArguments)
                        COLUMN_DATE + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);

                break;
            }

            //usado en el MainActivity, traer todas las filas
            case CODE_WEATHER: {
                cursor = infoClimaDatabaseHelper.getReadableDatabase().query(
                        "weather",
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /*
    Metodo utilizado para eliminar rows y cargar nuevas. Principalmente lo utiliza la clase que realiza
    la sincronizacion
    */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        //numero de files eliminadas, esto retornara el metodo
        int numRowsDeleted;

        //si le pasamos null aqui acorde a SQLite la tabla entera será eliminada. Pero si le pasamos
        // '1' eliminara todas las filas que encuentre y retornara el numero de filas eliminadas
        if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {

            case CODE_WEATHER:
                numRowsDeleted = infoClimaDatabaseHelper.getWritableDatabase().delete(
                        "weather",
                        selection,
                        selectionArgs);

                break;

            default:
                throw new UnsupportedOperationException("Uri desconocida: " + uri);
        }

        //si se eliminaron filas, notificamos al content resolver
        if (numRowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numRowsDeleted;
    }

    /*
    Los siguientes metodos no se utilizan para esta app pero es obligatorio que se implementen
     */

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
