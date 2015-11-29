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

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.preference.PreferenceManager

class Settings(ctx: Context)
{
	private var context: Context? = null

	private var prefs: SharedPreferences? = null

	init
	{
		context = ctx
		prefs = PreferenceManager.getDefaultSharedPreferences(context)
	}

	var isInitialPopulated: Boolean
		get() = prefs!!.getBoolean(INITIAL_POPULATED_KEY, false)
		set(value)
		{
			val editor = prefs!!.edit()
			editor.putBoolean(INITIAL_POPULATED_KEY, value)
			editor.commit()
		}

	var isServiceEnabled: Boolean
		get() = prefs!!.getBoolean(IS_ENABLED_KEY, false)
		set(value)
		{
			val editor = prefs!!.edit()
			editor.putBoolean(IS_ENABLED_KEY, value)
			editor.commit()
		}


	fun setOngoingNotification(value: Boolean)
	{
		val editor = prefs!!.edit()
		editor.putBoolean(ONGOING_NOTIFICATION_KEY, value)
		editor.commit()
	}

	val isOngoingNotificationEnabled: Boolean
		get() = prefs!!.getBoolean(ONGOING_NOTIFICATION_KEY, true)

	fun setSilencePeriod(from: Int, to: Int)
	{
		val editor = prefs!!.edit()

		editor.putInt(SILENCE_FROM_KEY, from)
		editor.putInt(SILENCE_TO_KEY, to)

		editor.commit()
	}

	// 21:00
	val silenceFrom: Int
		get() = prefs!!.getInt(SILENCE_FROM_KEY, 21 * 60 + 0)

	// 5:30
	val silenceTo: Int
		get() = prefs!!.getInt(SILENCE_TO_KEY, 5 * 60 + 30)

	fun hasSilencePeriod(): Boolean
	{
		return silenceFrom != silenceTo
	}

	val isSilencePeriodEnabled: Boolean
		get() = prefs!!.getBoolean(ENABLE_SILENCE_HOURS_KEY, false)

	// fail back
	var vibrationPattern: LongArray
		get()
		{
			val patternStr = prefs!!.getString(VIBRATION_PATTERN_KEY,
				defaultVibrationPatternStr)

			val pattern = parseVibrationPattern(patternStr)

			if (pattern != null)
				return pattern

			return defaultVibrationPattern
		}
		set(pattern)
		{
			val editor = prefs!!.edit()
			val sb = StringBuilder()

			var isFirst = true
			for (len in pattern)
			{
				if (!isFirst)
					sb.append(",")
				sb.append(len)
				isFirst = false
			}

			editor.putString(VIBRATION_PATTERN_KEY, sb.toString())
			editor.commit()
		}

	val ringtoneURI: Uri
		get()
		{
			var notification: Uri? = null
			try
			{
				val uriValue = prefs!!.getString(RINGTONE_KEY, "")

				if (uriValue != null && !uriValue.isEmpty())
					notification = Uri.parse(uriValue)
			}
			catch (e: Exception)
			{
				e.printStackTrace()
			}
			finally
			{
				if (notification == null)
					notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
			}

			return notification!!
		}

	companion object
	{

		private val IS_ENABLED_KEY = "pref_key_is_enabled"
		private val VIBRATION_PATTERN_KEY = "pref_key_vibration_pattern"
		private val RINGTONE_KEY = "pref_key_ringtone"

		private val ONGOING_NOTIFICATION_KEY = "pref_key_enable_ongoing_notification"

		// number of minutes since 00:00:00 
		private val SILENCE_FROM_KEY = "pref_key_time_silence_from"
		private val SILENCE_TO_KEY = "pref_key_time_silence_to"

		private val ENABLE_SILENCE_HOURS_KEY = "pref_key_enable_silence_hours"

		private val INITIAL_POPULATED_KEY = "initial_populated"

		val defaultVibrationPattern: LongArray
			get() = longArrayOf(0, 80, 30, 100, 40, 110, 50, 120, 50, 150, 30, 150, 150, 1500)

		val defaultVibrationPatternStr: String
			get()
			{
				val pattern = defaultVibrationPattern

				val sb = StringBuilder()

				var isFirst = true

				for (length in pattern)
				{
					if (!isFirst)
						sb.append(",")

					isFirst = false

					sb.append(length)
				}

				return sb.toString()
			}

		fun parseVibrationPattern(pattern: String): LongArray?
		{
			var ret: LongArray? = null

			try
			{
				val times = pattern.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

				ret = LongArray(times.size())

				for (idx in times.indices)
				{
					ret[idx] = java.lang.Long.parseLong(times[idx])
				}
			}
			catch (ex: Exception)
			{
				Lw.d("Got exception while parsing vibration pattern " + pattern)
				ret = null
			}

			return ret
		}
	}
}
