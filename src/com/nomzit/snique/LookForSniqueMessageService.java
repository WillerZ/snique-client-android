package com.nomzit.snique;

import java.security.InvalidKeyException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class LookForSniqueMessageService extends Service
{
	static final int LOOK_FOR_SNIQUE_MESSAGE = 1;
	final Messenger messenger = new Messenger(new IncomingHandler());
	
	abstract class AbstractMessageFinder extends Thread
	{
		private SniqueMessageDecoder decoder;
		private CodedMessage coded;
		
		AbstractMessageFinder(SniqueMessageDecoder decoder, CodedMessage coded)
		{
			super();
			this.decoder = decoder;
			this.coded = coded;
		}
		
		abstract void notifyMessage(SniqueMessage message);
				
		@Override
		public void run()
		{
			try
			{
				SniqueMessage message = decoder.decodeMessage(coded);
				this.notifyMessage(message);
			}
			catch (InvalidKeyException e)
			{
				Log.e("LookForSniqueMessageService.MessageFinder","Invalid Key",e);
			}
			catch (NoMessageException e)
			{
			}
			catch (WillNeverWorkException e)
			{
				Log.e("LookForSniqueMessageService.MessageFinder","Will never work",e);
			}
		}
	}
	
	private class CupcakeMessageFinder extends AbstractMessageFinder
	{
		public CupcakeMessageFinder(SniqueMessageDecoder decoder, CodedMessage coded)
		{
			super(decoder, coded);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		void notifyMessage(SniqueMessage message)
		{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Intent destroyMessageIntent = new Intent(LookForSniqueMessageService.this, DestroySniqueNotificationService.class);
			destroyMessageIntent.putExtra(DestroySniqueNotificationService.MESSAGE_ID,message.getId());
			PendingIntent pi = PendingIntent.getService(LookForSniqueMessageService.this, 0, destroyMessageIntent, 0);

			Notification notification = new Notification();
			notification.when = System.currentTimeMillis();
			notification.defaults = 0;
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.icon = R.drawable.statusbar;
			notification.setLatestEventInfo(LookForSniqueMessageService.this, "snique", message.getMessage(), pi);
			notificationManager.notify(message.getId(), notification);
		}
	}
	
	private class HoneycombMessageFinder extends AbstractMessageFinder
	{
		public HoneycombMessageFinder(SniqueMessageDecoder decoder, CodedMessage coded)
		{
			super(decoder, coded);
		}
		
		@Override
		void notifyMessage(SniqueMessage message)
		{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Intent destroyMessageIntent = new Intent(LookForSniqueMessageService.this,DestroySniqueNotificationService.class);
			destroyMessageIntent.putExtra(DestroySniqueNotificationService.MESSAGE_ID,message.getId());
			PendingIntent pi = PendingIntent.getService(LookForSniqueMessageService.this, 0, destroyMessageIntent, 0);

			Notification.Builder builder = new Notification.Builder(LookForSniqueMessageService.this);
			builder.setWhen(System.currentTimeMillis())
				   .setDefaults(0)
				   .setContentIntent(pi)
				   .setOnlyAlertOnce(true)
				   .setContentText(" ")
				   .setSmallIcon(R.drawable.statusbar)
				   ;
			
			notificationManager.notify(message.getId(), builder.getNotification());

			builder = new Notification.Builder(LookForSniqueMessageService.this);
			builder.setWhen(System.currentTimeMillis())
				   .setDefaults(0)
				   .setContentIntent(pi)
				   .setOnlyAlertOnce(true)
				   .setContentText(message.getMessage())
				   .setSmallIcon(R.drawable.statusbar);

			notificationManager.notify(message.getId(), builder.getNotification());
		}
	}
	
	private AbstractMessageFinder getMessageFinder(SniqueMessageDecoder decoder, CodedMessage coded)
	{
		final int Version = Build.VERSION.SDK_INT;
		if (Version < Build.VERSION_CODES.HONEYCOMB)
			return this.new CupcakeMessageFinder(decoder, coded);
		return this.new HoneycombMessageFinder(decoder, coded);
	}

	class IncomingHandler extends Handler
	{
		SniqueMessageDecoder decoder;
		private final byte keyRaw[] = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
			(byte) 0xdd, (byte) 0xee, (byte) 0xff };

		public IncomingHandler()
		{
			super();
			try
			{
				decoder = new SniqueMessageDecoder(keyRaw);
			}
			catch (WillNeverWorkException e)
			{
				Log.e("LookForSniqueMessageService.MessageFinder", "Will never work", e);
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			if (msg.what != LOOK_FOR_SNIQUE_MESSAGE)
			{
				super.handleMessage(msg);
				return;
			}
			Bundle bundle = msg.getData();
			CodedMessage message = new CodedMessage(bundle);
			AbstractMessageFinder finder = getMessageFinder(decoder, message);
			finder.start();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return messenger.getBinder();
	}

}
