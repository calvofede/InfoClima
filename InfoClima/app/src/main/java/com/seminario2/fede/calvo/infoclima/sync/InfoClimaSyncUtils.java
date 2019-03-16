package com.seminario2.fede.calvo.infoclima.sync;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.seminario2.fede.calvo.infoclima.utils.InfoClimaDateUtils;

import java.util.concurrent.TimeUnit;

import static com.seminario2.fede.calvo.infoclima.MainActivity.CONTENT_URI;

/**
 * Clase que se encargara de crear y trigerear el JobDispatcher para actualizar la tabla con nuevos
 * datos de Internet cada cierto tiempo.
 * La segunda funcion de esta clase es comprobar si existen datos en la tabla y si no es asi, llamar
 * inmediatamente a una sincronizacion de datos
 */
public class InfoClimaSyncUtils {

    private static boolean sInitialized;

    //el tiempo en el cual se ejecutara el job de 30 min a 1 hora. Esto esta en una ventana de tiempo porque
    //el Android decide cuando ejecutar el job dentro de ese time frame para economizar bateria, por ejemplo
    //ejecutando varios jobs de distintas aplicaciones que requieran conectarse a la red, en pocos minutos en vez
    //de estar abriendo y cerrando la conexion cuando el job lo pida
    private static final int SYNC_INTERVAL_MINUTES = 30;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(SYNC_INTERVAL_MINUTES);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS;

    private static final String SYNC_TAG = "infoClima-sync";

    static void scheduleFirebaseJobDispatcherSync(@NonNull final Context context) {

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        //creamos el job
        Job syncSunshineJob = dispatcher.newJobBuilder()
                //el servicio que llamará el JOB. Este servicio usa un AsyncTask para llamar a una sincronizacion
                //de los datos de la DB
                .setService(InfoClimaFirebaseJobService.class)
                //ID unico del JOB
                .setTag(SYNC_TAG)
                //Network constraint que indica la condicion de red para que se cree este JOB.
                //En este caso si el dispositivo esta por wifi o red de datos moviles
                .setConstraints(Constraint.ON_ANY_NETWORK)
                //tiempo por el cual el JOB debe persistir
                .setLifetime(Lifetime.FOREVER)
                //Hacemos que sea recurrente para mantener siempre actualizados los datos de la APP
                .setRecurring(true)
                //Ventana de tiempo en la cual tiene que ejecutarse el job y por ende la sincronizacion
                //el primer argumento indica el comienzo del tiempo y el segundo el tiempo maximo
                //en este caso será entre 30min y 1 hora de scheduleado el JOB.
                .setTrigger(Trigger.executionWindow(
                        SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                //Si ya hay un JOB con este tag corriendo, se reemplaza
                .setReplaceCurrent(true)
                .build();

        //scheduleamos el JOB
        dispatcher.schedule(syncSunshineJob);
    }

    /*
    Chequeamos periodicamente si necesitamos sincronizacion o no. Este metodo se llama en onCreate
    de MainActivity
     */
    synchronized public static void initialize(@NonNull final Context context) {

        if (sInitialized) {
            return;
        }

        sInitialized = true;

        //metodo para schedulear el JOB de sincronizacion
        scheduleFirebaseJobDispatcherSync(context);

        //Tenemos que chequear si el ContentProvider posee data para mostrar en la lista
        //Pero realizarlo en el main thread no es aconsejable porque podria causar LAG
        //asi que se crea un thread para correr la query y chequear el contenido del ContentProvider
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {


                Long dateFromToday = InfoClimaDateUtils.normalizeDate(System.currentTimeMillis());
                String selection = "date" + " >=" + dateFromToday;
                String[] projectionColumns = {"_id"};

                Cursor cursor = context.getContentResolver().query(
                        CONTENT_URI,
                        projectionColumns,
                        selection,
                        null,
                        null);

                /*
                El cursor podria ser NULL por diversos motivos
                URI invalida
                La query retorna NULL por el contentProvider
                Una excepcion

                Si es NULL o esta vacio tenemos que llamar inmediatamente para sincronizar los datos
                 */
                if (null == cursor || cursor.getCount() == 0) {
                    startImmediateSync(context);
                }

                //cerramos el cursor para evitar posibles memory leaks
                cursor.close();
                return null;
            }
        }.execute();

    }

    //llamamos al servicio de sincronizacion de los datos con un intent startService
    public static void startImmediateSync(@NonNull final Context context) {
        Intent intent = new Intent(context, InfoClimaSyncIntentService.class);
        context.startService(intent);
    }

}
