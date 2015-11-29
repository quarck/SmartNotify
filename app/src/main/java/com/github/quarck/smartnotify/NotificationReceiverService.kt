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

import java.util.ArrayList
import java.util.HashMap

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReceiverService : NotificationListenerService(), Handler.Callback
{

	private var alarm: Alarm? = null

	private val messenger = Messenger(Handler(this))

	private var settings: Settings? = null

	private var pkgSettings: PackageSettings? = null

	override fun handleMessage(msg: Message): Boolean
	{
		var ret = true

		Lw.d(TAG, "handleMessage, msg=" + msg.what)

		when (msg.what)
		{
			MSG_CHECK_PERMISSIONS -> ret = handleCheckPermissions(msg)

			MSG_LIST_NOTIFICATIONS -> ret = handleListNotifications(msg)

			MSG_RELOAD_SETTINGS ->
			{
				Lw.d(TAG, "Explicit request to reload config")
				update(null)
			}

			MSG_LIST_RECENT_NOTIFICATIONS ->
			{
				Lw.d(TAG, "Req for recent notifications")
				sendRecent(msg)
			}

			MSG_TOGGLE_MUTE ->
			{
				Lw.d(TAG, "Toggling mute")
				GlobalState.setIsMuted(this, !GlobalState.getIsMuted(this))
				update(null)
			}
		}

		return ret
	}

	private fun handleCheckPermissions(msg: Message): Boolean
	{
		Lw.d(TAG, "handleCheckPermissions")
		try
		{
			activeNotifications
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception, have no permissions!")
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0))
		}

		return true
	}

	private fun handleListNotifications(msg: Message): Boolean
	{
		Lw.d(TAG, "handleListNotifications")
		try
		{
			val notifications = activeNotifications
			val `val` = arrayOfNulls<String>(notifications.size())

			var idx = 0
			for (notification in notifications)
			{
				Lw.d(TAG, "Sending info about notification " + notification)
				`val`[idx++] = notification.packageName
			}

			reply(msg, Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, `val`))
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception, have no permissions!")
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0))
		}

		return true
	}

	private fun sendRecent(msg: Message): Boolean
	{
		Lw.d(TAG, "sendRecent")

		val notifications = getRecentNotifications()

		if (notifications != null)
			reply(msg, Message.obtain(null, MSG_LIST_RECENT_NOTIFICATIONS, 0, 0, notifications))

		return true
	}

	private fun reply(msgIn: Message, msgOut: Message)
	{
		try
		{
			msgIn.replyTo.send(msgOut)
		}
		catch (e: RemoteException)
		{
			e.printStackTrace()
		}

	}

	override fun onCreate()
	{
		super.onCreate()
		Lw.d(TAG, "onCreate()")

		Lw.d(TAG, "AlarmReceiver")
		alarm = Alarm()

		Lw.d(TAG, "Settings")
		settings = Settings(this)

		Lw.d(TAG, "PackageSettings")
		pkgSettings = PackageSettings(this)

		CallStateTracker.start(this)
	}

	override fun onDestroy()
	{
		Lw.d(TAG, "onDestroy (??)")
		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder?
	{
		if (intent.getBooleanExtra(configServiceExtra, false))
			return messenger.binder

		return super.onBind(intent)
	}

	private fun update(addedOrRemoved: StatusBarNotification?)
	{
		Lw.d(TAG, "update")

		if (addedOrRemoved != null && addedOrRemoved.packageName != Consts.packageName)
		{
			synchronized (NotificationReceiverService::class.java) {
				recentNotifications.put(
					addedOrRemoved.packageName,
					System.currentTimeMillis())

				if (recentNotifications.size > 100)
					cleanupRecentNotifications()
			}
		}

		if (!settings!!.isServiceEnabled)
		{
			Lw.d(TAG, "Service is disabled, cancelling all the alarms and returning")
			alarm!!.cancelAlarm(this)
			if (GlobalState.getLastCountHandledNotifications(this) != 0)
			{
				OngoingNotificationManager.cancelOngoingNotification(this)
				GlobalState.setLastCountHandledNotifications(this, 0)
			}
			return
		}

		var notifications: Array<StatusBarNotification>? = null

		try
		{
			notifications = activeNotifications
		}
		catch (ex: NullPointerException)
		{
			Lw.e(TAG, "Got exception while obtaining list of notifications, have no permissions!")
		}

		var cntHandledNotifications = 0

		var minReminderInterval = Integer.MAX_VALUE

		if (notifications != null)
		{
			Lw.d(TAG, "Total number of notifications currently active: " + notifications.size())

			for (notification in notifications)
			{
				Lw.d(TAG, "Checking notification" + notification)
				val packageName = notification.packageName

				if (packageName == Consts.packageName)
				{
					Lw.d(TAG, "That's ours, ignoring")
					continue
				}

				Lw.d(TAG, "Package name is " + packageName)

				val pkg = pkgSettings!!.getPackage(packageName)

				if (pkg != null && pkg.isHandlingThis)
				{
					Lw.d(TAG, "We are handling this!")

					++cntHandledNotifications
					if (pkg.remindIntervalSeconds < minReminderInterval)
					{
						minReminderInterval = pkg.remindIntervalSeconds
						Lw.d(TAG, "remind interval updated to $minReminderInterval seconds")
					}
				}
				else
				{
					if (pkg == null)
						Lw.d(TAG, "No settings for package " + packageName)
					else
						Lw.d(TAG, "There are settings for packageName: $pkg, but it is not currently handled")
				}
			}

			Lw.d(TAG, "Currently known packages: ")
			for (pkg in pkgSettings!!.allPackages)
			{
				Lw.d(TAG, ">> " + pkg)
			}
		}
		else
		{
			Lw.e(TAG, "Can't get list of notifications. WE HAVE NO PERMISSION!! ")
		}

		if (cntHandledNotifications != 0)
		{
			Lw.d(TAG, "(Re)Setting alarm with interval $minReminderInterval seconds")
			alarm!!.setAlarmMillis(this, minReminderInterval * 1000)

			if ((GlobalState.getLastCountHandledNotifications(this) != cntHandledNotifications) && settings!!.isOngoingNotificationEnabled)
			{
				GlobalState.setLastCountHandledNotifications(this, cntHandledNotifications)
				OngoingNotificationManager.showUpdateOngoingNotification(this)
			}
		}
		else if (GlobalState.getIsMuted(this))
		// if we are muted - always show the notification also
		{
			Lw.d(TAG, "Nothing to notify about, cancelling alarm, if any, but setting notification since we are muted")
			alarm!!.cancelAlarm(this)

			if ((GlobalState.getLastCountHandledNotifications(this) != cntHandledNotifications) && settings!!.isOngoingNotificationEnabled)
			{
				GlobalState.setLastCountHandledNotifications(this, 0)
				OngoingNotificationManager.showUpdateOngoingNotification(this)
			}
		}
		else
		{
			Lw.d(TAG, "Nothing to notify about, cancelling alarm, if any")
			alarm!!.cancelAlarm(this)

			if (GlobalState.getLastCountHandledNotifications(this) != 0)
			{
				GlobalState.setLastCountHandledNotifications(this, 0)
				OngoingNotificationManager.cancelOngoingNotification(this)
			}
		}
	}

	override fun onNotificationPosted(arg0: StatusBarNotification)
	{
		Lw.d(TAG, "Notification posted: " + arg0)
		update(arg0)
	}

	override fun onNotificationRemoved(arg0: StatusBarNotification)
	{
		Lw.d(TAG, "Notification removed: " + arg0)
		update(arg0)
	}

	companion object
	{
		val TAG = "Service"

		val configServiceExtra = "configService"

		val MSG_CHECK_PERMISSIONS = 1
		val MSG_NO_PERMISSIONS = 2
		val MSG_LIST_NOTIFICATIONS = 3
		val MSG_RELOAD_SETTINGS = 4
		val MSG_LIST_RECENT_NOTIFICATIONS = 5
		val MSG_TOGGLE_MUTE = 6

		private val recentNotifications = HashMap<String, Long>()

		private fun cleanupRecentNotifications()
		{
			val timeNow = System.currentTimeMillis()

			val listToCleanup = ArrayList<String>()

			for (entry in recentNotifications.entries)
			{
				val key = entry.key
				val value = entry.value

				if (timeNow - value.toLong() > 1000 * 3600 * 24)
				// older than 1 day
				{
					listToCleanup.add(key)
					recentNotifications.remove(key)
				}
			}

			for (key in listToCleanup)
				recentNotifications.remove(key)
		}

		fun getRecentNotifications(): Array<String>
		{
			Lw.d(TAG, "getRecentNotifications")

			var notifications: Array<String?>? = null

			synchronized (NotificationReceiverService::class.java) {
				cleanupRecentNotifications()

				notifications = arrayOfNulls<String>(recentNotifications.size)

				var idx = 0
				for (key in recentNotifications.keys)
				{
					notifications!![idx++] = key
				}
			}

			return notifications!!
				.filter {it != null}
				.map {it!!}
				.toTypedArray()
		}
	}
}
