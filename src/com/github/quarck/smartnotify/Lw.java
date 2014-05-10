package com.github.quarck.smartnotify;

import android.util.Log;

public class Lw
{
	private final static String TAG_PREFIX = "SmartNotify ";

	public static void d(String TAG, String message)
	{
		Log.d(TAG_PREFIX + TAG, "" + System.currentTimeMillis() + " " + message);
	}
	
	public static void d(String message)
	{
		d("<NOTAG>", message);
	}

	public static void e(String TAG, String message)
	{
		Log.e(TAG_PREFIX + TAG, "" + System.currentTimeMillis() + " " + message);
	}

	public static void e(String message)
	{
		e("<NOTAG>", message);
	}
}
