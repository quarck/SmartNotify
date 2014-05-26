/*
 * Copyright (c) 2014, Sergey Parshin, quarck@gmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.github.quarck.smartnotify;

import android.app.Application;
import android.content.Context;

public class GlobalState extends Application
{
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
	

	private boolean isOnCall = false;
	
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
	
	
	private long lastFireTime = 0;

	public static long getLastFireTime(Context ctx)
	{
		return instance(ctx).lastFireTime;
	}

	public static void setLastFireTime(Context ctx, long lastFireTime)
	{
		instance(ctx).lastFireTime = lastFireTime;
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
