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

import java.util.Calendar

object SilentPeriodManager
{
	private val TAG = "SilentPeriodManager"

	fun isEnabled(settings: Settings): Boolean
	{
		return settings.isSilencePeriodEnabled && settings.hasSilencePeriod()
	}

	// returns time in millis, when silent period ends, 
	// or 0 if we are not on silent 
	fun getSilentUntil(settings: Settings): Long
	{
		if (!isEnabled(settings))
			return 0

		var ret: Long = 0

		val cal = Calendar.getInstance()
		val hour = cal.get(Calendar.HOUR_OF_DAY)
		val minute = cal.get(Calendar.MINUTE)
		val currentTm = hour * 60 + minute

		val silenceFrom = settings.silenceFrom
		var silenceTo = settings.silenceTo

		Lw.d(TAG, "have silent period from $silenceFrom to $silenceTo")
		Lw.d(TAG, "Current time is " + currentTm)

		if (silenceTo < silenceFrom)
			silenceTo += 24 * 60

		if (inRange(currentTm, silenceFrom, silenceTo) || inRange(currentTm + 24 * 60, silenceFrom, silenceTo))
		{
			val silentLenghtMins = ((silenceTo + 24 * 60 - currentTm) % (24 * 60)).toLong()

			ret = System.currentTimeMillis() + silentLenghtMins * 60 * 1000 // convert minutes to milliseconds

			Lw.d(TAG, "We are in the silent zone until $ret (it is $silentLenghtMins minutes from now)")
		}
		else
		{
			Lw.d(TAG, "We are not in the silent mode")
		}

		return ret
	}

	private fun inRange(value: Int, low: Int, high: Int): Boolean
	{
		return (low <= value && value <= high)
	}
}
