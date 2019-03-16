package com.seminario2.fede.calvo.infoclima.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Una sublase Intent Service para manejar request task asincronas, en un thread separado
 */
public class InfoClimaSyncIntentService extends IntentService {

    public InfoClimaSyncIntentService() {
        super("InfoClimaSyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        InfoClimaSyncTask.syncWeather(this);
    }
}
