package com.github.quarck.smartnotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class OngoingNotificationManager
{
	public static void showUpdateOngoingNotification(Context ctx)
	{
		if (! new Settings(ctx).isOngoingNotificationEnabled())
		{
			cancelOngoingNotification(ctx);
			return;
		}
		
		Intent intent = new Intent(ctx, ToggleMuteBroadcastReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		Intent mainActivityIntent = new Intent(ctx, MainActivity.class);
		PendingIntent pendingMainActivityIntent = PendingIntent.getActivity(ctx, 0, mainActivityIntent, 0);
		
		RemoteViews view = new RemoteViews(ctx.getPackageName(), 
				!GlobalState.getIsMuted(ctx) ? R.layout.notification_view : R.layout.notification_view_muted);	
		
		view.setOnClickPendingIntent(R.id.buttonMute, pendingIntent);
		if (!GlobalState.getIsMuted(ctx))
		{
			int cntActive = GlobalState.getLastCountHandledNotifications(ctx);
			long intervalMin = GlobalState.getCurrentRemindInterval(ctx)/60/1000; 
					
			StringBuilder sb = new StringBuilder();
			
			sb.append(cntActive);
			sb.append(" ");
			if (cntActive != 1) 
				sb.append(ctx.getString(R.string.notification_hint_apps));
			else
				sb.append(ctx.getString(R.string.notification_hint_app));
			sb.append(ctx.getString(R.string.notification_hint_every));
			sb.append(" ");
			sb.append(intervalMin);
			sb.append(" ");
			if (intervalMin != 1)
				sb.append(ctx.getString(R.string.notification_hint_mins));
			else
				sb.append(ctx.getString(R.string.notification_hint_min));

			view.setTextViewText(R.id.textViewSmallText, sb.toString() );
		}
		
		Notification ongoingNotification = new Notification.Builder(ctx)
	         .setContent(view)
	         .setSmallIcon(R.drawable.ic_notification)
	         .setOngoing(true)
	         .setPriority(Notification.PRIORITY_MIN)
	         .setContentIntent(pendingMainActivityIntent)
	         .build();
	
		((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE))
			.notify(Consts.notificationIdOngoing, ongoingNotification); // would update if already exists
	}
	
	public static void cancelOngoingNotification(Context ctx)
	{
		((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE))
			.cancel(Consts.notificationIdOngoing);
	}

	public static void updateNotification(Context ctx)
	{
		if ( GlobalState.getLastCountHandledNotifications(ctx) > 0 
				|| GlobalState.getIsMuted(ctx) )
		{
			showUpdateOngoingNotification(ctx);
		}
		else
		{
			cancelOngoingNotification(ctx);
		}
	}
}
