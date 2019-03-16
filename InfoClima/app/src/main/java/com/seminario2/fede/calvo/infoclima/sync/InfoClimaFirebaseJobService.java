package com.seminario2.fede.calvo.infoclima.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * Llamado que realiza una vez ejecutado el JOB. Mediante el AsyncTask se sincroniza el clima
 * asincronicamente
 */
public class InfoClimaFirebaseJobService extends JobService {

    private AsyncTask<Void, Void, Void> fetchWeatherTask;

    /*
    Al iniciar el JOB llamamos inmediatamente para sincronizar el clima, al finalizar,
    en onPostExecute damos por finalizado el JOB
     */
    @Override
    public boolean onStartJob(final JobParameters job) {
        fetchWeatherTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Context context = getApplicationContext();
                InfoClimaSyncTask.syncWeather(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(job, false);
            }
        };

        fetchWeatherTask.execute();
        return true;
    }

    /*
        Se llama cuando el sistema decide interrumpir el JOB que esta corriendo.
        Probablemente porque las constraints asociadas al JOB ya no se estan dando, por ejemplo,
        necesitabamos que el dispositivo unicamente esté por WIFI y ya no se cumple esa condicion,
        por lo tanto, cancelamos el JOB en curso con el metodo cancel del AsyncTask
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        if (fetchWeatherTask != null) {
            fetchWeatherTask.cancel(true);
        }
        return true;
    }
}
