package com.github.quarck.smartnotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ToggleMuteBroadcastReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		GlobalState.setIsMuted(context, !GlobalState.getIsMuted(context))

		OngoingNotificationManager.updateNotification(context)
	}
}
