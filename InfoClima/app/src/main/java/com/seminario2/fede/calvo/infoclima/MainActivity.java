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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.seminario2.fede.calvo.infoclima.sync.InfoClimaSyncUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaDateUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaPreferences;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements InfoClimaAdapter.InfoClimaAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    //Content URI del content provider que utilizamos para acceder a la tabla WEATHER de la app
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + "com.seminario2.fede.calvo.infoclima")
                    .buildUpon()
                    .appendPath("weather")
                    .build();

    //Nombre de columnas para utilizar con las QUERIES
    protected static String[] CONTENT_PROVIDER_COLUMN_NAMES = {
            "date",
            "weather_id",
            "min",
            "max",
            "humidity",
            "pressure",
            "wind",
            "degrees",
            "summary",
            "weekSummary",
            "weekIcon",
            "lastUpdate"
    };

    //Indices de columnas para traer los datos del cursor
    protected static int DATE_COLUMN_INDEX = 0;
    protected static int WEATHER_ID_COLUMN_INDEX = 1;
    protected static int MIN_COLUMN_INDEX = 2;
    protected static int MAX_COLUMN_INDEX = 3;
    protected static int HUMIDITY_COLUMN_INDEX = 4;
    protected static int PRESSURE_COLUMN_INDEX = 5;
    protected static int WIND_COLUMN_INDEX = 6;
    protected static int DEGREES_COLUMN_INDEX = 7;
    protected static int SUMMARY_COLUMN_INDEX = 8;
    protected static int WEEK_SUMMARY_COLUMN_INDEX = 9;
    protected static int WEEK_ICON_COLUMN_INDEX = 10;
    protected static int LAST_UPDATE_COLUMN_INDEX = 11;

    //ID del loader, debe ser unico
    private static final int LOADER_ID = 1;

    //Key que usaremos para enviarle al activity Details para que lo tome y realice la Query
    public static final String INTENT_EXTRA_TO_QUERY_W_DETAILS = "dateInMilliseconds";

    private ProgressBar progressBar;
    private static int mPosition = RecyclerView.NO_POSITION;
    private static final String DATE_COLUMN_NAME = "date";
    private RecyclerView recyclerView;
    private InfoClimaAdapter infoClimaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerView = findViewById(R.id.recyclerview_weather);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        infoClimaAdapter = new InfoClimaAdapter(this, this);
        recyclerView.setAdapter(infoClimaAdapter);

        progressBar = findViewById(R.id.progress_bar);
        showLoading();

        //inicializa el loader o bien el ultimo loader creado es reutilizado
        getSupportLoaderManager().initLoader(LOADER_ID, null, MainActivity.this);

        /**
         *Inicializa el JobDispatcher para que de 2 a 3 horas llame a un metodo para la sincronizacion de datos y
         *descargue nuevos datos del clima.
         *
         *La segunda funcion que realiza este metodo es hacer una query a la tabla y chequear si el
         * cursor que retorna esta vacio o no. Si esta vacio inmediatamente llama a otro metodo para
         * que se sincronicen los datos, llamando a la API y bajando nuevos datos del clima y guardandolos
         * en la tabla.
         */
        InfoClimaSyncUtils.initialize(this);

    }

    //Enviamos un Intent al activity Details para que mediante este KEY saque la fecha en milisegundos
    //para realizar la query
    @Override
    public void onClick(Long dateInMilliseconds) {
        Intent intent = new Intent(this, WeatherDetailActivity.class);
        intent.putExtra(INTENT_EXTRA_TO_QUERY_W_DETAILS, dateInMilliseconds);

        startActivity(intent);
    }

    /*
    Se llama a este metodo cuando un nuevo Loader tiene que ser creado.
    Aqui se realiza una QUERY a la tabla weather del content provider para traernos los datos del clima
    del dia de HOY en adelante
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable final Bundle bundle) {

        if (i == LOADER_ID) {

            String sortOrder = DATE_COLUMN_NAME + " ASC";
            Long dateFromToday = InfoClimaDateUtils.normalizeDate(System.currentTimeMillis());
            String selection = DATE_COLUMN_NAME + " >=" + dateFromToday;

            return new CursorLoader(this,
                    CONTENT_URI,
                    CONTENT_PROVIDER_COLUMN_NAMES,
                    selection,
                    null,
                    sortOrder);
        } else {
            throw new RuntimeException("Loader: " + LOADER_ID + "not defined. Error!");
        }

    }

    /*
    Se lo llama cuando el loader terminó de cargar la data
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {

        infoClimaAdapter.swapCursor(cursor);

        if (mPosition == RecyclerView.NO_POSITION) {
            mPosition = 0;
        }

        recyclerView.smoothScrollToPosition(mPosition);

        if (cursor.getCount() != 0) {
            showWeatherData();
        }
    }

    /*
    Se lo llama cuando un loader creado previamente esta siendo reseteado, haciendo su data innacesible
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        infoClimaAdapter.swapCursor(null);
    }

    /*
    Inflamos el menu. Debemos retornar TRUE para que se muestre el menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    /**
     * Método para abrir una aplicacion de mapas con la location
     * seleccionada en las Settings
     */
    private void openMapLocation() {
        if (!InfoClimaPreferences.getLastLocationFoundCorrectly(this).equalsIgnoreCase(
                InfoClimaPreferences.getWeatherPreferredLocation(this))) {
            Toast.makeText(this, this.getString(R.string.error_unable_to_shot_map_location),
                    Toast.LENGTH_LONG).show();

            return;
        }

        //traemos las coordenadas de las settings
        String coords = InfoClimaPreferences.getLocationCoordinates(this);

        //creamos la URI que recibira la APP de mapa que abrira este intent
        Uri geoLocation = Uri.parse("geo:" + coords);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(MainActivity.class.getSimpleName(), "Error! " + geoLocation.toString()
                    + ", no hay APP que soporte esta solicitud");
        }
    }

    /*
    Callback que se llama cuando un menu item fue seleccionado
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.map_action == item.getItemId()) {
            openMapLocation();
            return true;
        }

        if (R.id.settings_action == item.getItemId()) {
            openSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
    }

    private void showWeatherData() {
        progressBar.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}
