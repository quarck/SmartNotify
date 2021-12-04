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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object CommonAppsRegistry
{
	private val packages = arrayOf<String>(
		"com.google.android.calendar",
		"com.android.phone",
		"com.android.mms",
		"com.android.calendar",
		"com.google.android.gm",
		"com.viber.voip",
		"com.whatsapp",
	)

	private var initialized = false
	private var numFoundApps = 0
	private var applicationInfos: Array<ApplicationInfo?> = arrayOf()

	fun initRegistry(ctx: Context) {
		if (initialized)
			return

		val pMap = BooleanArray(packages.size)
		val appInfos = arrayOfNulls<ApplicationInfo>(packages.size)
		var found = 0

		val packageManager = ctx.packageManager

		for (i in packages.indices) {
			val pkg = packages[i]
			try {
				val app = packageManager.getApplicationInfo(pkg, 0/*PackageManager.GET_META_DATA*/)
				pMap[i] = true
				appInfos[i] = app
				found++
			} catch (ex: Exception) {
			}

		}

		applicationInfos = appInfos
		numFoundApps += found
		initialized = true
	}

	val numApplications: Int
		get() {
			return numFoundApps
		}

	val applications: Array<ApplicationInfo>
		get() = applicationInfos.filterNotNull().toTypedArray()
}
