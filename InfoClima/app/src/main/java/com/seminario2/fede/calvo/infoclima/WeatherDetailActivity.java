package com.seminario2.fede.calvo.infoclima;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.seminario2.fede.calvo.infoclima.darkSkyAPIUtils.DarkSkyAPIUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaDateUtils;

import static com.seminario2.fede.calvo.infoclima.MainActivity.CONTENT_URI;
import static com.seminario2.fede.calvo.infoclima.MainActivity.INTENT_EXTRA_TO_QUERY_W_DETAILS;

/**
 * Activity que muestra detalle del clima seleccionado en MainActivity
 */
public class WeatherDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

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

    private static final int LOADER_ID = 20122018;

    private String weatherDetailSummary;

    private Long dateInMilliseconds;

    private TextView textViewDate;
    private TextView textViewDescription;
    private TextView textViewHighTemp;
    private TextView textViewLowTemp;
    private TextView textViewHumidity;
    private TextView textViewWind;
    private TextView textViewPressure;

    private ImageView ivIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_detail);

        ivIcon = findViewById(R.id.weather_icon);
        textViewDate = findViewById(R.id.date);
        textViewDescription = findViewById(R.id.weather_description);
        textViewHighTemp = findViewById(R.id.high_temperature);
        textViewLowTemp = findViewById(R.id.low_temperature);
        textViewHumidity = findViewById(R.id.humidity);
        textViewWind = findViewById(R.id.wind_measurement);
        textViewPressure = findViewById(R.id.pressure);

        Intent intent = getIntent();

        dateInMilliseconds = intent.getLongExtra(INTENT_EXTRA_TO_QUERY_W_DETAILS, 0L);

        //si no recibimos la fecha en milisegundos desde la activity anterior, tiramos NPE.
        //No se puede mostrar la fecha seleccionada
        if (dateInMilliseconds == 0L) {
            throw new NullPointerException();
        }

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(LOADER_ID, null, WeatherDetailActivity.this);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // "inflamos" el xml de menu que corresponde a esta activity
        getMenuInflater().inflate(R.menu.weather_detail_menu, menu);

        return true;
    }

    /**
     * Esta Activity cuenta con dos items de menu. Para compartir el clima eligiendo la app y
     * acceder a la activity de configuracion
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.share_action == item.getItemId()) {
            Intent intentShare = shareWeatherDetail();
            startActivity(Intent.createChooser(intentShare, null));
            return true;
        }

        if (R.id.settings_action == item.getItemId()) {
            openSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Intent que permite compartir el texto mostrado en esta activity
     * mediante distintas aplicaciones instaladas en el dispositivo
     */
    private Intent shareWeatherDetail() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_weather));
        i.putExtra(Intent.EXTRA_TEXT, weatherDetailSummary);

        return i;
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Loader que se encarga de realizar la QUERY para traer los detalles del clima.
     * Lo busca por medio de la columna DATE. Este campo DATE con tipo de dato Long es el que
     * vamos a recibir desde MainActivity, la DATE de la fila seleccionada para ver los detalles
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {

        if (i == LOADER_ID) {

            Uri uriQuery = CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(dateInMilliseconds))
                    .build();

            return new CursorLoader(this,
                    uriQuery,
                    CONTENT_PROVIDER_COLUMN_NAMES,
                    null,
                    null,
                    null);
        } else {
            throw new RuntimeException("Loader: " + LOADER_ID + "no definido!. Error!");
        }
    }

    /**
     * Al terminar la carga del Loader, preguntamos si el cursor es distinto de NULL y si es asi
     * lo movemos a la primera posicion. Solo tendra una posicion porque estamos trayendo un solo y unico
     * registro.
     * Luego traemos los datos de las distintas columnas para completar los Views de la activity
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        if (cursor != null && cursor.moveToFirst()) {

            String date = InfoClimaDateUtils.getFriendlyDateString(
                    this, cursor.getLong(DATE_COLUMN_INDEX), false);

            String lowTemp = DarkSkyAPIUtils.formatTemperature(this, cursor.getDouble(MIN_COLUMN_INDEX));
            String highTemp = DarkSkyAPIUtils.formatTemperature(this, cursor.getDouble(MAX_COLUMN_INDEX));

            float wind = cursor.getFloat(WIND_COLUMN_INDEX);
            float degrees = cursor.getFloat(DEGREES_COLUMN_INDEX);
            String windSpeedAndDirection = DarkSkyAPIUtils.getFormattedWind(this, wind, degrees);

            String weatherDesc = cursor.getString(SUMMARY_COLUMN_INDEX);

            weatherDesc = TextUtils.substring(weatherDesc, 0, weatherDesc.length() - 1);

            String pressure = String.valueOf(cursor.getFloat(PRESSURE_COLUMN_INDEX)).concat(" hPa");

            String humidity = String.valueOf(cursor.getInt(HUMIDITY_COLUMN_INDEX)).concat(" %");

            int weatherImage = DarkSkyAPIUtils.getArtResourceForWeatherCondition(
                    cursor.getString(WEATHER_ID_COLUMN_INDEX));

            ivIcon.setImageResource(weatherImage);
            textViewDate.setText(date);
            textViewDescription.setText(weatherDesc);
            textViewHighTemp.setText(highTemp);
            textViewLowTemp.setText(lowTemp);
            textViewHumidity.setText(humidity);
            textViewWind.setText(windSpeedAndDirection);
            textViewPressure.setText(pressure);

            //Esta variable es utilizada para el boton de ENVIAR que permite compartir el texto de
            //este detalle del clima entre varias aplicaciones
            weatherDetailSummary = date + " - " + weatherDesc + " - " + "Max: " + highTemp + " Min: " + lowTemp;
        }

    }

    //Al no guardar la data del cursor, no es necesario remover la data de un loader previo, de lo que
    //se encarga este metodo
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
}
