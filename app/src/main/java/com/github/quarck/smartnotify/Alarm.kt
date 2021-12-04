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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Vibrator

class Alarm : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		Lw.d(TAG, "Alarm received")

		val settings = Settings(context)

		if (settings.isServiceEnabled)
		{
			Lw.d(TAG, "Serivce is enabled")

			var fireReminder = true

			if (SilentPeriodManager.isEnabled(settings))
			{
				val tmUntilEnd = SilentPeriodManager.getSilentUntil(settings)

				if (tmUntilEnd != 0L)
				{
					Lw.d(TAG, "Service is enabled, but we are in silent zone, not alarming!")
					fireReminder = false
				}
				else
				{
					Lw.d(TAG, "Service is enabled, we are not in silient zone, vibrating.")
				}
			}
			else
			{
				Lw.d(TAG, "Service is enabled, no silet period. vibrating")
			}

			if (fireReminder && GlobalState.getIsOnCall(context))
			{
				Lw.d(TAG, "Was going to fire the reminder, but call is currently in progress. Would skip this one.")
				fireReminder = false
			}

			if (fireReminder && GlobalState.getIsMuted(context))
			{
				Lw.d(TAG, "Service is enabled, but muted, not firing")
				fireReminder = false
			}

			val currentTime = System.currentTimeMillis()
			val lastFireTime = GlobalState.getLastFireTime(context)

			if (fireReminder && (currentTime - lastFireTime < 60 * 1000))
			// do not fire more often than once a minute
			{
				fireReminder = false
			}

			if (fireReminder)
			{
				val fired = checkPhoneSilentAndFire(context, settings)

				if (fired)
					GlobalState.setLastFireTime(context, lastFireTime)
			}
		}
		else
		{
			Lw.d(TAG, "Service is now got disabled, cancelling alarm")
			cancelAlarm(context)
		}
	}

	private fun checkPhoneSilentAndFire(ctx: Context, settings: Settings): Boolean
	{
		var mayFireVibration = false
		var mayFireSound = false

		var fired = false

		val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

		when (am.ringerMode)
		{
			AudioManager.RINGER_MODE_SILENT -> Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_SILENT")
			AudioManager.RINGER_MODE_VIBRATE ->
			{
				Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_VIBRATE")
				mayFireVibration = true
			}
			AudioManager.RINGER_MODE_NORMAL ->
			{
				Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_NORMAL")
				mayFireVibration = true
				mayFireSound = true
			}
		}

		if (mayFireVibration)
		{
			Lw.d(TAG, "Firing vibro-alarm finally")

			fired = true

			val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
			val pattern = settings.vibrationPattern
			v.vibrate(pattern, -1)
		}

		if (mayFireSound)
		{
			Lw.d(TAG, "Playing sound notification, if URI is not null")

			fired = true

			try
			{
				val notificationUri = settings.ringtoneURI

				val mediaPlayer = MediaPlayer()

				mediaPlayer.setDataSource(ctx, notificationUri)
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
				mediaPlayer.prepare()
				mediaPlayer.setOnCompletionListener { mp -> mp.release() }
				mediaPlayer.start()
			}
			catch (e: Exception)
			{
				Lw.e(TAG, "Exception while playing notification")
				e.printStackTrace()
			}

		}

		return fired
	}

	fun setAlarmMillis(context: Context, repeatMillis: Int)
	{
		Lw.d(TAG, "Setting alarm with repeation interval $repeatMillis milliseconds")

		if (GlobalState.getCurrentRemindInterval(context) != repeatMillis.toLong())
		{
			Lw.d(TAG, "Cancelling previous alarm since interval has changed or new alarm has been introduced")
			val cancelIntent = Intent(context, Alarm::class.java)
			val sender = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE)

			alarmManager(context).cancel(sender)

			Lw.d(TAG, "Setting up new alarm with interval " + repeatMillis)

			val intent = Intent(context, Alarm::class.java)
			val pendIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

			alarmManager(context).setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + repeatMillis, repeatMillis.toLong(), pendIntent)

			GlobalState.setCurrentRemindInterval(context, repeatMillis.toLong())
		}
		else
		{
			Lw.d(TAG, "Alarm interval didn't change - NOT TOUCHING THE ALARM")
		}
	}

	fun cancelAlarm(context: Context)
	{
		Lw.d(TAG, "Cancelling alarm")
		val intent = Intent(context, Alarm::class.java)
		val sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

		alarmManager(context).cancel(sender)

		GlobalState.setCurrentRemindInterval(context, 0)
	}

	private fun alarmManager(context: Context): AlarmManager
	{
		return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
	}

	companion object
	{
		val TAG = "Alarm"
	}

}
