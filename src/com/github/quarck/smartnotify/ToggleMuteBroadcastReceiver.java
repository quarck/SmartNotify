package com.github.quarck.smartnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ToggleMuteBroadcastReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		GlobalState.setIsMuted(context, ! GlobalState.getIsMuted(context));
		
		OngoingNotificationManager.updateNotification(context);
	}
}
