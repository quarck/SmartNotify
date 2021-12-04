//
//   Calendar Notifications Plus
//   Copyright (C) 2016 Sergey Parshin (s.parshin.sc@gmail.com)
//
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation; either version 3 of the License, or
//   (at your option) any later version.
//
//   This program is distributed in the hope that it will be useful,
//   but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.
//
//   You should have received a copy of the GNU General Public License
//   along with this program; if not, write to the Free Software Foundation,
//   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
//


package com.github.quarck.smartnotify

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import 	androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.quarck.smartnotify.PermissionsManager.hasPermission

object PermissionsManager {
    private fun Context.hasPermission(perm: String) =
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;

    private fun Activity.shouldShowRationale(perm: String) =
            ActivityCompat.shouldShowRequestPermissionRationale(this, perm)

    fun hasAllPermissions(context: Context): Boolean {
        val hasReadPackages =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                context.hasPermission(Manifest.permission.QUERY_ALL_PACKAGES)
            else
                true
        val hasReadPhoneState = context.hasPermission(Manifest.permission.READ_PHONE_STATE)
        return hasReadPhoneState && hasReadPackages
    }

    fun shouldShowRationale(activity: Activity): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.shouldShowRationale(Manifest.permission.QUERY_ALL_PACKAGES)) {
                return true
            }
        }
        return activity.shouldShowRationale(Manifest.permission.READ_PHONE_STATE)
    }

    fun requestPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.QUERY_ALL_PACKAGES), 0)

        } else {
            ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
        }
    }
}