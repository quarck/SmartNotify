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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException

class ServiceClient(val callback: Callback?) : Handler.Callback
{

	private var mService: Messenger? = null

	private var mIsBound: Boolean = false

	private val mMessenger = Messenger(Handler(this))

	interface Callback
	{
		fun onNoPermissions()

		fun onNotificationList(notificationList: Array<String>)

		fun onRecetNotificationsList(recentNotifications: Array<String>)
	}

	override fun handleMessage(msg: Message): Boolean
	{
		Lw.d(TAG, "handleMessage: " + msg.what)

		when (msg.what)
		{
			NotificationReceiverService.MSG_LIST_NOTIFICATIONS -> callback?.onNotificationList(msg.obj as Array<String>)
			NotificationReceiverService.MSG_LIST_RECENT_NOTIFICATIONS -> callback?.onRecetNotificationsList(msg.obj as Array<String>)
			NotificationReceiverService.MSG_NO_PERMISSIONS -> callback?.onNoPermissions()
		}

		if (callback == null)
			Lw.e(TAG, "No callback attached")

		return true
	}

	private val mConnection = object : ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			Lw.d(TAG, "Got connection to the service")

			mService = Messenger(service)
		}

		override fun onServiceDisconnected(className: ComponentName)
		{
			Lw.d(TAG, "Service disconnected")

			mService = null
		}
	}

	fun bindService(ctx: Context)
	{
		Lw.d(TAG, "Binding service")

		val it = Intent(ctx, NotificationReceiverService::class.java)
		it.putExtra(NotificationReceiverService.configServiceExtra, true)

		ctx.bindService(it, mConnection, Context.BIND_AUTO_CREATE)
		mIsBound = true
	}

	fun unbindService(ctx: Context)
	{
		if (mIsBound)
		{
			Lw.d(TAG, "unbinding service")
			ctx.unbindService(mConnection)
			mIsBound = false
		}
		else
		{
			Lw.e(TAG, "unbind request, but service is not bound!")
		}
	}

	private fun sendServiceReq(code: Int)
	{
		Lw.d(TAG, "Sending request $code to the service")
		if (mIsBound)
		{
			if (mService != null)
			{
				try
				{
					val msg = Message.obtain(null, code)
					msg.replyTo = mMessenger
					mService!!.send(msg)
				}
				catch (e: RemoteException)
				{
					Lw.e(TAG, "Failed to send req - got exception " + e)
				}

			}
		}
		else
		{
			Lw.e(TAG, "Failed to send req - service is not bound!")
		}
	}

	fun getListNotifications()
	{
		sendServiceReq(NotificationReceiverService.MSG_LIST_NOTIFICATIONS)
	}

	fun getListRecentNotifications()
	{
		sendServiceReq(NotificationReceiverService.MSG_LIST_RECENT_NOTIFICATIONS)
	}

	fun checkPermissions()
	{
		sendServiceReq(NotificationReceiverService.MSG_CHECK_PERMISSIONS)
	}

	fun forceReloadConfig()
	{
		sendServiceReq(NotificationReceiverService.MSG_RELOAD_SETTINGS)
	}

	fun toggleMute()
	{
		sendServiceReq(NotificationReceiverService.MSG_TOGGLE_MUTE)
	}

	companion object
	{
		private val TAG = "ServiceClient"
	}
}
