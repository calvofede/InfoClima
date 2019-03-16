package com.seminario2.fede.calvo.infoclima;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.seminario2.fede.calvo.infoclima.darkSkyAPIUtils.DarkSkyAPIUtils;
import com.seminario2.fede.calvo.infoclima.sync.InfoClimaSyncUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaDateUtils;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaPreferences;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.seminario2.fede.calvo.infoclima.MainActivity.DATE_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.LAST_UPDATE_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.MAX_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.MIN_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.SUMMARY_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.WEATHER_ID_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.WEEK_ICON_COLUMN_INDEX;
import static com.seminario2.fede.calvo.infoclima.MainActivity.WEEK_SUMMARY_COLUMN_INDEX;

/**
 * Adapter para ser usado con RecyclerView y de esta forma
 * mostrar views para cada hora del dia.
 * Una de las ventajas de recycler view es que permite manejar clicks en items de forma sencilla
 */
public class InfoClimaAdapter extends RecyclerView.Adapter<InfoClimaAdapter.InfoClimaAdapterViewHolder> {

    private static final int VIEW_TYPE_FIRST_ELEMENT = 0;
    private static final int VIEW_TYPE_NON_FIRST_ELEMENT = 1;

    private final Context mContext;
    private Cursor mCursor;

    private InfoClimaAdapterOnClickHandler weatherAdapterOnClickHandler;

    interface InfoClimaAdapterOnClickHandler {
        void onClick(Long date);
    }

    public InfoClimaAdapter(InfoClimaAdapterOnClickHandler infoClimaAdapterOnClickHandler, Context context) {
        weatherAdapterOnClickHandler = infoClimaAdapterOnClickHandler;
        mContext = context;
    }

    public class InfoClimaAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final ImageView weatherIcon;
        public final ImageView weekIcon;

        public final TextView description;
        public final TextView date;
        public final TextView minTemp;
        public final TextView maxTemp;
        public final TextView weekSummary;
        public final TextView lastUpdated;
        public final TextView location;

        public InfoClimaAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            description = itemView.findViewById(R.id.tv_weather_description);
            date = itemView.findViewById(R.id.tv_weather_date);
            weatherIcon = itemView.findViewById(R.id.iv_weather_icon);
            minTemp = itemView.findViewById(R.id.tv_weather_min_temp);
            maxTemp = itemView.findViewById(R.id.tv_weather_max_temp);
            weekSummary = itemView.findViewById(R.id.week_summary);
            lastUpdated = itemView.findViewById(R.id.last_update);
            location = itemView.findViewById(R.id.location);
            weekIcon = itemView.findViewById(R.id.week_icon);

            //le seteo un onClickListener a este viewHolder
            itemView.setOnClickListener(this);
        }

        // posicionamos el cursor en la posicion del adapter de la lista (en donde hicimos click)
        // y traemos la fecha en milisegundos para posteriormente en el activity details hacer query
        // a esa fecha y mostrar el detalle del clima
        @Override
        public void onClick(View v) {
            if (getAdapterPosition() == 0) {
                return;
            }
            mCursor.moveToPosition(getAdapterPosition() - 1);
            long dateInMilliseconds = mCursor.getLong(DATE_COLUMN_INDEX);
            weatherAdapterOnClickHandler.onClick(dateInMilliseconds);
        }
    }

    /*
    Este metodo se llama cuando un ViewHolder es creado para alimentar al recyclerView.
    Se crearan tantos ViewHolder se necesiten para cubrir la pantalla y algunos mas para scrolling
     */
    @NonNull
    @Override
    public InfoClimaAdapter.InfoClimaAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        int layoutId = 1;

        if (i == VIEW_TYPE_FIRST_ELEMENT) {
            layoutId = R.layout.clima_list_first_item;
        } else if (i == VIEW_TYPE_NON_FIRST_ELEMENT) {
            layoutId = R.layout.clima_list_item;
        }

        LayoutInflater li = LayoutInflater.from(mContext);

        //En false para que el inflated layout no se atachee inmediatamente al parent view group
        boolean attachToRoot = false;

        //toma el id de un layout xml y luego convierte este id a una collection de view groups
        View view = li.inflate(layoutId, viewGroup, attachToRoot);

        if (InfoClimaPreferences.getLanguage(mContext).equals("def")) {
            InfoClimaPreferences.saveLanguage(mContext, Locale.getDefault().getLanguage());
        } else if (!InfoClimaPreferences.getLanguage(mContext).equals(Locale.getDefault().getLanguage())) {
            InfoClimaPreferences.saveLanguage(mContext, Locale.getDefault().getLanguage());
            InfoClimaSyncUtils.startImmediateSync(mContext);
        }

        InfoClimaAdapterViewHolder viewHolder = new InfoClimaAdapterViewHolder(view);

        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_FIRST_ELEMENT;
        } else {
            return VIEW_TYPE_NON_FIRST_ELEMENT;
        }
    }

    /*
        Este metodo es llamado por el RecyclerView para mostrar la data en la posicion especifica.
        Nos llega el argumento "position" y aqui llenamos los datos de cada fila de la lista que se muestra
        en la activity principal.
        Recorremos las columnas del cursor que previamente movimos a la position con 'moveToPosition()'
        y seteamos los valores de los Views del Adapter
         */
    @Override
    public void onBindViewHolder(@NonNull InfoClimaAdapter.InfoClimaAdapterViewHolder infoClimaAdapterViewHolder, int i) {
        mCursor.moveToPosition(i);

        /*
        Preguntamos si es la primera posicion, lo que significa que se "infló" el el XML llamado clima_list_first_item
        por lo que completamos sus textView e imageView

        Traemos la location segun las settings para mostrar como primer elemento de la lista
        Seteamos el texto a mostrarse segun la ultima vez actualizado el clima. Ej: "hace 40 minutos"
        La descripcion del clima semanal y el icono del clima semanal
         */
        if (getItemViewType(i) == VIEW_TYPE_FIRST_ELEMENT) {
            String weatherIcon = mCursor.getString(WEEK_ICON_COLUMN_INDEX);
            int weatherImageId = DarkSkyAPIUtils.getArtResourceForWeatherCondition(weatherIcon);
            infoClimaAdapterViewHolder.weekIcon.setImageResource(weatherImageId);

            String location;
            if (!InfoClimaPreferences.getLastLocationFoundCorrectly(mContext).equalsIgnoreCase(
                    InfoClimaPreferences.getWeatherPreferredLocation(mContext))) {
                location = mContext.getString(R.string.error_location_not_found).concat(" ")
                        .concat(InfoClimaPreferences.getWeatherPreferredLocation(mContext)
                                .concat(" ").concat(mContext.getString(R.string.error_location_not_found2)));
            } else {
                location = InfoClimaPreferences.getWeatherPreferredLocation(mContext);
            }
            infoClimaAdapterViewHolder.location.setText(location);

            long lastTimeWeatherNetworkUpdated = mCursor.getLong(LAST_UPDATE_COLUMN_INDEX);

            String stringDateTimeSinceLastUpdate;

            long timeSinceLastUpdate = System.currentTimeMillis() - lastTimeWeatherNetworkUpdated;
            if (TimeUnit.MILLISECONDS.toMinutes(timeSinceLastUpdate) < 1) {
                stringDateTimeSinceLastUpdate = mContext.getString(R.string.updated_less_one_minute_ago);
            } else {
                stringDateTimeSinceLastUpdate = DateUtils.getRelativeTimeSpanString(
                        lastTimeWeatherNetworkUpdated).toString().toLowerCase();
            }

            String lastUpdatedTextView = mContext.getString(R.string.updated)
                    .concat(" ")
                    .concat(stringDateTimeSinceLastUpdate);
            infoClimaAdapterViewHolder.lastUpdated.setText(lastUpdatedTextView);

            String weekSummary;
            if (!InfoClimaPreferences.getLastLocationFoundCorrectly(mContext).equalsIgnoreCase(
                    InfoClimaPreferences.getWeatherPreferredLocation(mContext))) {
                weekSummary = mContext.getString(R.string.error_weather_unknown_desc);
            } else {
                weekSummary = mCursor.getString(WEEK_SUMMARY_COLUMN_INDEX);

            /*
            Buscamos la primer ocurrencia de un digito y del simbolo ° en el texto del
            resument del clima semanal para reemplazar ese valor
            por ejemplo "31°C" por el correspondiente en F o C dependiendo de la setting del usuario
             */
                Pattern numbers = Pattern.compile("[0-9]");
                Pattern symbol = Pattern.compile("°");
                Matcher mNumber = numbers.matcher(weekSummary);
                Matcher mSymbol = symbol.matcher(weekSummary);

                //Chequeamos si encuentra alguna ocurrencia de un digito Y del simbolo ° en el texto
                if (mNumber.find() && mSymbol.find()) {

                    int firstIndexNumber = mNumber.start();
                    int firstIndexSymbol = mSymbol.start();

                    String temperatureWithoutSymbol = weekSummary.substring(firstIndexNumber, firstIndexSymbol);
                    String temperatureWithSymbol = weekSummary.substring(firstIndexNumber, firstIndexSymbol + 2);

                    //Formateamos la temperatura sin el simbolo, ej: "31"
                    String newTemperatureWithSymbol = DarkSkyAPIUtils.formatTemperature(mContext, Double.valueOf(temperatureWithoutSymbol));

                    //Reemplazamos la vieja temperatura con el simbolo "31°C" por la correcta. Si el usuario
                    //selecciono Imperial será reemplazada por "88°F". Y sino se mantendra la misma.
                    weekSummary = weekSummary.replace(temperatureWithSymbol, newTemperatureWithSymbol);

                    /*
                    Removemos el ultimo punto del texto
                    */
                    weekSummary = TextUtils.substring(weekSummary, 0, weekSummary.length() - 1);
                }
            }

            infoClimaAdapterViewHolder.weekSummary.setText(weekSummary);
        }


        if (getItemViewType(i) == VIEW_TYPE_NON_FIRST_ELEMENT) {
            mCursor.moveToPosition(i - 1);

            String weatherIcon = mCursor.getString(WEATHER_ID_COLUMN_INDEX);
            int weatherImageId = DarkSkyAPIUtils.getArtResourceForWeatherCondition(weatherIcon);
            infoClimaAdapterViewHolder.weatherIcon.setImageResource(weatherImageId);

            long date = mCursor.getLong(DATE_COLUMN_INDEX);
            //Metodo para convertir la fecha de milisegundos a un formato agradable a la vista.
            //Como por ejemplo: "Hoy, 25 de noviembre"
            String dateString = InfoClimaDateUtils.getFriendlyDateString(mContext, date, false);
            infoClimaAdapterViewHolder.date.setText(dateString);

            String description = mCursor.getString(SUMMARY_COLUMN_INDEX);
            infoClimaAdapterViewHolder.description.setText(description);

            double tempCelsius = mCursor.getDouble(MAX_COLUMN_INDEX);
            //Metodo para convertir la temperatura dependiendo de la setting que haya seleccionado el usuario
            //Expresada en Celsius (C) al seleccionar Metrico o Fahrenheit (F) al elegir Imperial.
            String highTempString = DarkSkyAPIUtils.formatTemperature(mContext, tempCelsius);
            infoClimaAdapterViewHolder.maxTemp.setText(highTempString);

            double minTempCelsius = mCursor.getDouble(MIN_COLUMN_INDEX);
            String minTempString = DarkSkyAPIUtils.formatTemperature(mContext, minTempCelsius);
            infoClimaAdapterViewHolder.minTemp.setText(minTempString);
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        } else {
            return mCursor.getCount();
        }
    }

    /*
    Metodo utilizado por el InfoClimaAdapter para la data. Este metodo es llamado por el MainActivity
    despues que la carga de datos ha finalizado. Cuando este metodo es llamado, se asume que tenemos un
    nuevo set de datos por lo que llamamos a notifyDataSetChanged para que el RecyclerView actualice
     */
    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

}
