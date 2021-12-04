package com.github.quarck.smartnotify

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppUpdatedBroadcastReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		// after each update we are loosing permission to get notifications,
		// so service actually gets disabled, update settings to reflect this and 
		// then - ask user to re-enable permission for us		
		Settings(context).isServiceEnabled = false

		val mainActivityIntent = Intent(context, MainActivity::class.java)
		val pendingMainActivityIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

		val notification = Notification.Builder(context).setContentTitle(context.getString(R.string.app_updated)).setContentText(context.getString(R.string.reenable_app)).setSmallIcon(R.drawable.ic_circle_notifications_black_48dp).setPriority(Notification.PRIORITY_HIGH).setContentIntent(pendingMainActivityIntent).setAutoCancel(true).build()

		(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(Consts.notificationIdUpdated, notification) // would update if already exists
	}
}
