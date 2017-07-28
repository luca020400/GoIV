package com.kamron.pogoiv.pokeflycomponents;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.NotificationCompat;

import com.kamron.pogoiv.MainActivity;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.ScreenGrabber;

/**
 * Created by johan on 2017-07-06.
 * <p>
 * An object which can display a running or paused notification for goiv
 */

public class GoIVNotificationManager {

    private static final int NOTIFICATION_REQ_CODE = 8959;

    private static Pokefly pokefly;

    public static final String ACTION_RECALIBRATE_SCANAREA = "com.kamron.pogoiv.ACTION_RECALIBRATE_SCANAREA";

    public GoIVNotificationManager(Pokefly pokefly) {
        this.pokefly = pokefly;
    }


    /**
     * Show a paused notification in the system notification tray.
     */
    public void showPausedNotification() {

        Intent openAppIntent = new Intent(pokefly, MainActivity.class);

        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                pokefly, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startSettingAppIntent = new Intent(pokefly, MainActivity.class);
        startSettingAppIntent.setAction(MainActivity.ACTION_OPEN_SETTINGS);

        PendingIntent startSettingsPendingIntent = PendingIntent.getActivity(
                pokefly, 0, startSettingAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action startSettingsAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_settings_white_24dp,
                pokefly.getString(R.string.settings_page_title),
                startSettingsPendingIntent).build();


        Intent startAppIntent = new Intent(pokefly, MainActivity.class);

        startAppIntent.setAction(MainActivity.ACTION_START_POKEFLY);

        PendingIntent startServicePendingIntent = PendingIntent.getActivity(
                pokefly, 0, startAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action startServiceAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_play_arrow_white_24px,
                pokefly.getString(R.string.main_start),
                startServicePendingIntent).build();

        Notification notification = new NotificationCompat.Builder(pokefly.getApplicationContext())
                .setOngoing(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setColor(pokefly.getColorC(R.color.colorAccent))
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(pokefly.getString(R.string.notification_title_goiv_stopped))
                .setContentText(pokefly.getString(R.string.notification_title_tap_to_open))
                .setContentIntent(openAppPendingIntent)
                .addAction(startSettingsAction)
                .addAction(startServiceAction)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        NotificationManager mNotifyMgr =
                (NotificationManager) pokefly.getSystemService(pokefly.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(NOTIFICATION_REQ_CODE, notification);
    }

    /**
     * Show a running notification in the system notification tray.
     */
    public void showRunningNotification() {

        Intent openAppIntent = new Intent(pokefly, MainActivity.class);

        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                pokefly, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent recalibrateScreenScanningIntent = new Intent(pokefly, NotificationActionService.class);

        recalibrateScreenScanningIntent.setAction(ACTION_RECALIBRATE_SCANAREA);

        PendingIntent recalibrateScreenScanningPendingIntent = PendingIntent.getService(
                pokefly, 0, recalibrateScreenScanningIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action recalibrateScreenScanAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_add_white_24px,
                "Recalibrate scanner",
                recalibrateScreenScanningPendingIntent).build();

        Intent stopServiceIntent = new Intent(pokefly, Pokefly.class);
        stopServiceIntent.setAction(pokefly.ACTION_STOP);

        PendingIntent stopServicePendingIntent = PendingIntent.getService(
                pokefly, 0, stopServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action stopServiceAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_pause_white_24px,
                pokefly.getString(R.string.pause_goiv_notification),
                stopServicePendingIntent).build();

        Notification notification = new NotificationCompat.Builder(pokefly)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setColor(pokefly.getColorC(R.color.colorPrimary))
                .setSmallIcon(R.drawable.notification_icon_play)
                .setContentTitle(pokefly.getString(R.string.notification_title, pokefly.getTrainerLevel()))
                .setContentText(pokefly.getString(R.string.notification_title_tap_to_open))
                .setContentIntent(openAppPendingIntent)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(recalibrateScreenScanAction)
                .addAction(stopServiceAction)
                .build();

        pokefly.startForeground(NOTIFICATION_REQ_CODE, notification);
    }

    /**
     * The class which receives the intent to recalibrate the scan area.
     */
    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            if (ACTION_RECALIBRATE_SCANAREA.equals(action)) {

                Handler handler = new Handler(Looper.getMainLooper());
                Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                pokefly.sendBroadcast(closeIntent);


                final Runnable r = new Runnable() {
                    public void run() {
                        Bitmap bmp = ScreenGrabber.getInstance().grabScreen();
                        pokefly.getOcr().recalibrateScanAreas(bmp);

                    }
                };
                handler.postDelayed(r, 2000);


            }
        }
    }
}