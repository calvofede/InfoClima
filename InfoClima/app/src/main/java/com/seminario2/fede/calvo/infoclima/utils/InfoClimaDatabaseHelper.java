package com.seminario2.fede.calvo.infoclima.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Clase para manejar la base de datos local para la APP
 */
public class InfoClimaDatabaseHelper extends SQLiteOpenHelper {
    //nombre del archivo de base de datos
    public static final String DATABASE_NAME = "weather.db";

    //verion de la base de datos. Al cambiar este numero de elimina la tabla y se crea el schema nuevamente
    private static final int DATABASE_VERSION = 7;

    public InfoClimaDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /*
    Llamado cuando la tabla se crea por primera vez
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

         //creamos la tabla para cachear nuestra data del clima
        final String SQL_CREATE_WEATHER_TABLE =

                "CREATE TABLE " + "weather" + " (" +


                        "_id" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        "date" + " INTEGER NOT NULL, " +

                        "weather_id" + " TEXT NOT NULL," +

                        "min" + " REAL NOT NULL, " +
                        "max" + " REAL NOT NULL, " +
                        "summary" + " TEXT NOT NULL," +

                        "humidity" + " REAL NOT NULL, " +
                        "pressure" + " REAL NOT NULL, " +

                        "wind" + " REAL NOT NULL, " +
                        "degrees" + " REAL NOT NULL, " +

                        "weekSummary" + " TEXT NOT NULL," +
                        "weekIcon" + " TEXT NOT NULL," +
                        "lastUpdate" + " TEXT NOT NULL," +

                        //reemplazamos la fila si queremos insertar una y ya existe esa fecha.
                        //de este modo nos aseguramos tener registros con fechas unicas
                        " UNIQUE (" + "date" + ") ON CONFLICT REPLACE);";

        sqLiteDatabase.execSQL(SQL_CREATE_WEATHER_TABLE);
    }

    /*
    Metodo que detecta cambios en el numero de DATABASE_VERSION y si es asi elimina la tabla
    y la crea nuevamente con el schema actualizado
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "weather");
        onCreate(sqLiteDatabase);
    }
}
