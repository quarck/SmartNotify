package com.github.quarck.smartnotify;

import android.app.Application;

public class GlobalState extends Application
{
	private boolean isOnCall = false;
	
	public void setIsOnCall(boolean val)
	{
		isOnCall = val;
	}
	
	public boolean getIsOnCall()
	{
		return isOnCall;
	}
}
