package com.harout.flextomaps;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MapsNotificationListenerService extends NotificationListenerService {

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (MainActivity.began && sbn.getPackageName().equalsIgnoreCase("com.google.android.apps.maps")) {
            MainActivity.sendAddressesToMaps(MainActivity.context);
        }
    }
}