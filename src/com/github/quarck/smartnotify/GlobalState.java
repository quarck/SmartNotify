package com.github.quarck.smartnotify;

import android.app.Application;
import android.content.Context;

public class GlobalState extends Application
{
	private boolean isOnCall = false;
	
	private static GlobalState _instance = null;
	
	public static GlobalState instance(Context ctx)
	{
		if (_instance == null)
		{
			synchronized(GlobalState.class)
			{
				if (_instance == null)
					_instance = (GlobalState) ctx.getApplicationContext();
			}
		}
		
		return _instance;
	}
	
	public void setIsOnCall(boolean val)
	{
		isOnCall = val;
	}
	
	public boolean getIsOnCall()
	{
		return isOnCall;
	}

	public static void setIsOnCall(Context ctx, boolean val)
	{
		instance(ctx).isOnCall = val;
	}
	
	public static boolean getIsOnCall(Context ctx)
	{
		return instance(ctx).isOnCall;
	}
	
	private long nextAlarmTime = 0;
	
	public static void setNextAlarmTime(Context ctx, long val)
	{
		instance(ctx).nextAlarmTime = val;
	}
	
	public static long getNextAlarmTime(Context ctx)
	{
		return instance(ctx).nextAlarmTime;
	}

	private long currentRemindInterval = 0;
	
	public static void setCurrentRemindInterval(Context ctx, long val)
	{
		instance(ctx).currentRemindInterval = val;
	}
	
	public static long getCurrentRemindInterval(Context ctx)
	{
		return instance(ctx).currentRemindInterval;
	}
}
