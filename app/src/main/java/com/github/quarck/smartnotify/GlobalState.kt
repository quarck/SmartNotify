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


package com.github.quarck.smartnotify

import android.app.Application
import android.content.Context

public class GlobalState : Application()
{
	var isOnCall = false


	private var isMuted = false

	private var lastCountHandledNotifications = -1


	private var lastFireTime: Long = 0


	private var currentRemindInterval: Long = 0

	companion object
	{
		private var _instance: GlobalState? = null

		fun instance(ctx: Context): GlobalState
		{
			if (_instance == null)
			{
				synchronized (GlobalState::class.java) {
					if (_instance == null)
						_instance = ctx.applicationContext as GlobalState
				}
			}

			return _instance!!
		}


		fun setIsOnCall(ctx: Context, `val`: Boolean)
		{
			instance(ctx).isOnCall = `val`
		}

		fun getIsOnCall(ctx: Context): Boolean
		{
			return instance(ctx).isOnCall
		}

		fun setIsMuted(ctx: Context, `val`: Boolean)
		{
			instance(ctx).isMuted = `val`
		}

		fun getIsMuted(ctx: Context): Boolean
		{
			return instance(ctx).isMuted
		}

		fun getLastCountHandledNotifications(ctx: Context): Int
		{
			return instance(ctx).lastCountHandledNotifications
		}

		fun setLastCountHandledNotifications(ctx: Context, lastCountHandledNotifications: Int)
		{
			instance(ctx).lastCountHandledNotifications = lastCountHandledNotifications
		}

		fun getLastFireTime(ctx: Context): Long
		{
			return instance(ctx).lastFireTime
		}

		fun setLastFireTime(ctx: Context, lastFireTime: Long)
		{
			instance(ctx).lastFireTime = lastFireTime
		}

		fun setCurrentRemindInterval(ctx: Context, `val`: Long)
		{
			instance(ctx).currentRemindInterval = `val`
		}

		fun getCurrentRemindInterval(ctx: Context): Long
		{
			return instance(ctx).currentRemindInterval
		}
	}
}
