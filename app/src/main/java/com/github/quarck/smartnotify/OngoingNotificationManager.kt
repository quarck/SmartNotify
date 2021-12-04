package com.github.quarck.smartnotify

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object OngoingNotificationManager
{
	fun showUpdateOngoingNotification(ctx: Context)
	{
		if (!Settings(ctx).isOngoingNotificationEnabled)
		{
			cancelOngoingNotification(ctx)
			return
		}

		val intent = Intent(ctx, ToggleMuteBroadcastReceiver::class.java)
		val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

		val mainActivityIntent = Intent(ctx, MainActivity::class.java)
		val pendingMainActivityIntent = PendingIntent.getActivity(ctx, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

		val view = RemoteViews(ctx.packageName,
			if (!GlobalState.getIsMuted(ctx)) R.layout.notification_view else R.layout.notification_view_muted)

		view.setOnClickPendingIntent(R.id.buttonMute, pendingIntent)
		if (!GlobalState.getIsMuted(ctx))
		{
			val cntActive = GlobalState.getLastCountHandledNotifications(ctx)
			val intervalMin = GlobalState.getCurrentRemindInterval(ctx) / 60 / 1000

			val sb = StringBuilder()

			sb.append(cntActive)
			sb.append(" ")
			if (cntActive != 1)
				sb.append(ctx.getString(R.string.notification_hint_apps))
			else
				sb.append(ctx.getString(R.string.notification_hint_app))
			sb.append(ctx.getString(R.string.notification_hint_every))
			sb.append(" ")
			sb.append(intervalMin)
			sb.append(" ")
			if (intervalMin != 1L)
				sb.append(ctx.getString(R.string.notification_hint_mins))
			else
				sb.append(ctx.getString(R.string.notification_hint_min))

			view.setTextViewText(R.id.textViewSmallText, sb.toString())
		}

		val ongoingNotification = Notification.Builder(ctx).setContent(view).setSmallIcon(R.drawable.ic_circle_notifications_black_48dp).setOngoing(true).setPriority(Notification.PRIORITY_MIN).setContentIntent(pendingMainActivityIntent).build()

		(ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(Consts.notificationIdOngoing, ongoingNotification) // would update if already exists
	}

	fun cancelOngoingNotification(ctx: Context)
	{
		(ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(Consts.notificationIdOngoing)
	}

	fun updateNotification(ctx: Context)
	{
		if (GlobalState.getLastCountHandledNotifications(ctx) > 0 || GlobalState.getIsMuted(ctx))
		{
			showUpdateOngoingNotification(ctx)
		}
		else
		{
			cancelOngoingNotification(ctx)
		}
	}
}
