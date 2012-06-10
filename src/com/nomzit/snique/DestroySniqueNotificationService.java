package com.nomzit.snique;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class DestroySniqueNotificationService extends Service
{
	public static final String MESSAGE_ID = "messageId";
	
	@Override
	public IBinder onBind(Intent intent)
	{
		int messageId = intent.getIntExtra(MESSAGE_ID, 0);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(messageId);
		return null;
	}

}
