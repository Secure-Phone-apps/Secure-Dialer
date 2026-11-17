/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.util

import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher
import java.util.Locale

enum class OemFamily {
    XIAOMI_HYPEROS,
    SAMSUNG_ONEUI,
    OPPO_REALME_ONEPLUS,
    VIVO_IQOO,
    HUAWEI_HONOR,
    TRANSSION,
    ASUS,
    LENOVO,
    MOTOROLA,
    NOTHING,
    GENERIC_AOSP
}

object CallScreenPermissionHelper {

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun detectOemFamily(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty()
    ): OemFamily {
        val signature = "$manufacturer $brand".lowercase(Locale.ROOT)
        return when {
            "xiaomi" in signature || "redmi" in signature || "poco" in signature || "blackshark" in signature ->
                OemFamily.XIAOMI_HYPEROS
            "samsung" in signature ->
                OemFamily.SAMSUNG_ONEUI
            "oppo" in signature || "realme" in signature || "oneplus" in signature || "oplus" in signature ->
                OemFamily.OPPO_REALME_ONEPLUS
            "vivo" in signature || "iqoo" in signature ->
                OemFamily.VIVO_IQOO
            "huawei" in signature || "honor" in signature ->
                OemFamily.HUAWEI_HONOR
            "tecno" in signature || "infinix" in signature || "itel" in signature || "transsion" in signature ->
                OemFamily.TRANSSION
            "asus" in signature || "rog" in signature ->
                OemFamily.ASUS
            "lenovo" in signature ->
                OemFamily.LENOVO
            "motorola" in signature || "moto" in signature ->
                OemFamily.MOTOROLA
            "nothing" in signature ->
                OemFamily.NOTHING
            else -> OemFamily.GENERIC_AOSP
        }
    }

    fun getOemDisplayName(oemFamily: OemFamily): String {
        return when (oemFamily) {
            OemFamily.XIAOMI_HYPEROS -> "Xiaomi / HyperOS / MIUI"
            OemFamily.SAMSUNG_ONEUI -> "Samsung One UI"
            OemFamily.OPPO_REALME_ONEPLUS -> "Oppo / Realme / OnePlus"
            OemFamily.VIVO_IQOO -> "Vivo / iQOO"
            OemFamily.HUAWEI_HONOR -> "Huawei / Honor"
            OemFamily.TRANSSION -> "Tecno / Infinix / itel"
            OemFamily.ASUS -> "ASUS"
            OemFamily.LENOVO -> "Lenovo"
            OemFamily.MOTOROLA -> "Motorola"
            OemFamily.NOTHING -> "Nothing OS"
            OemFamily.GENERIC_AOSP -> "Android System"
        }
    }

    fun requiresAggressiveOemWhitelist(oemFamily: OemFamily): Boolean {
        return when (oemFamily) {
            OemFamily.XIAOMI_HYPEROS,
            OemFamily.SAMSUNG_ONEUI,
            OemFamily.OPPO_REALME_ONEPLUS,
            OemFamily.VIVO_IQOO,
            OemFamily.HUAWEI_HONOR,
            OemFamily.TRANSSION,
            OemFamily.ASUS,
            OemFamily.LENOVO -> true
            OemFamily.MOTOROLA,
            OemFamily.NOTHING,
            OemFamily.GENERIC_AOSP -> false
        }
    }

    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            } else {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                tm?.defaultDialerPackage == context.packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    if (findActivity(context) == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}

            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    if (findActivity(context) == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        openAppDetailsSettings(context)
    }

    fun getDefaultDialerIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        @Suppress("DEPRECATION")
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    fun requestDefaultDialer(context: Context, launcher: ActivityResultLauncher<Intent>? = null) {
        val intent = getDefaultDialerIntent(context)
        if (intent != null) {
            try {
                if (launcher != null) {
                    launcher.launch(intent)
                    return
                }
                val activity = findActivity(context)
                if (activity != null) {
                    activity.startActivity(intent)
                    return
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }

        // Fallback for restricted OEMs (HyperOS, ColorOS, Nothing OS): open Default Apps system settings
        try {
            val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                if (findActivity(context) == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(defaultAppsIntent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return isDefaultDialer(context)
        }
        return try {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } catch (_: Exception) {
            false
        }
    }

    fun getCallScreeningRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        return null
    }

    fun requestCallScreeningRole(context: Context, launcher: ActivityResultLauncher<Intent>? = null) {
        val intent = getCallScreeningRoleIntent(context)
        if (intent != null) {
            try {
                if (launcher != null) {
                    launcher.launch(intent)
                    return
                }
                val activity = findActivity(context)
                if (activity != null) {
                    activity.startActivity(intent)
                    return
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
        try {
            val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                if (findActivity(context) == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(defaultAppsIntent)
        } catch (_: Exception) {
            openAppDetailsSettings(context)
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    openAppDetailsSettings(context)
                }
            }
        }
    }

    fun requestFullScreenIntentPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        openChannelNotificationSettings(context)
    }

    fun openChannelNotificationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, "incoming_call_channel_v4")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        openAppDetailsSettings(context)
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Builds an ordered list of OEM-specific autostart, sleeping-app, and background battery settings
     * [Intent]s for the given [OemFamily], ending with standard AOSP battery & app-details fallbacks.
     */
    fun getOemBackgroundIntentCandidates(
        context: Context,
        oemFamily: OemFamily = detectOemFamily()
    ): List<Intent> {
        val pkg = context.packageName
        val appLabel = try {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        } catch (_: Exception) {
            "Secure Dialer"
        }

        val candidates = mutableListOf<Intent>()

        fun componentIntent(pkgName: String, clsName: String, extras: Intent.() -> Unit = {}): Intent {
            return Intent().apply {
                setClassName(pkgName, clsName)
                extras()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        when (oemFamily) {
            OemFamily.XIAOMI_HYPEROS -> {
                candidates += componentIntent(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                candidates += componentIntent(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                ) {
                    putExtra("package_name", pkg)
                    putExtra("package_label", appLabel)
                }
                candidates += Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", pkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            OemFamily.SAMSUNG_ONEUI -> {
                candidates += componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                )
                candidates += componentIntent(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
                candidates += componentIntent(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }

            OemFamily.OPPO_REALME_ONEPLUS -> {
                candidates += componentIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
                candidates += componentIntent(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
                candidates += componentIntent(
                    "com.oplus.battery",
                    "com.oplus.powermanager.fuelgaue.PowerControlActivity"
                )
                candidates += componentIntent(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
                candidates += componentIntent(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }

            OemFamily.VIVO_IQOO -> {
                candidates += componentIntent(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
                candidates += componentIntent(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
                candidates += componentIntent(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
                candidates += componentIntent(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.PurviewTabActivity"
                ) {
                    putExtra("packagename", pkg)
                }
            }

            OemFamily.HUAWEI_HONOR -> {
                candidates += componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
                candidates += componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
                candidates += componentIntent(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
                candidates += componentIntent(
                    "com.hihonor.systemmanager",
                    "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }

            OemFamily.TRANSSION -> {
                candidates += componentIntent(
                    "com.transsion.phonemaster",
                    "com.cyin.himgr.autostart.AutoStartActivity"
                )
                candidates += componentIntent(
                    "com.itel.autobootmanager",
                    "com.itel.autobootmanager.activity.AutoBootMgrActivity"
                )
            }

            OemFamily.ASUS -> {
                candidates += componentIntent(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.autostart.AutoStartActivity"
                )
                candidates += componentIntent(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.entry.FunctionActivity"
                )
            }

            OemFamily.LENOVO -> {
                candidates += componentIntent(
                    "com.lenovo.powersetting",
                    "com.lenovo.powersetting.ui.Settings\$HighPowerApplicationsActivity"
                )
            }

            OemFamily.MOTOROLA,
            OemFamily.NOTHING,
            OemFamily.GENERIC_AOSP -> Unit
        }

        // Standard Android Doze & Battery Optimization screens
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations(context)) {
            candidates += Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            candidates += Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        candidates += Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", pkg, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return candidates
    }

    /**
     * Launches the highest-priority resolvable & exported OEM autostart/battery survivability screen,
     * catching SecurityException cleanly across Android 9–17 ROM variants.
     */
    fun openOemBackgroundSettings(
        context: Context,
        oemFamily: OemFamily = detectOemFamily()
    ): Boolean {
        val pm = context.packageManager
        val candidates = getOemBackgroundIntentCandidates(context, oemFamily)
        for (intent in candidates) {
            try {
                val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?: pm.resolveActivity(intent, 0)
                if (resolved != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: SecurityException) {
                // Non-exported OEM component on newer Android release; try next candidate
            } catch (_: Exception) {
            }
        }
        openAppDetailsSettings(context)
        return false
    }

    fun openOemPermissionSettings(context: Context): Boolean {
        // 1. Xiaomi / HyperOS / MIUI Permission Editor (for popup/lock-screen/restricted permissions)
        try {
            val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (context.packageManager.resolveActivity(miuiIntent, 0) != null) {
                context.startActivity(miuiIntent)
                return true
            }
        } catch (_: Exception) {}

        try {
            val miuiIntentAlt = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (context.packageManager.resolveActivity(miuiIntentAlt, 0) != null) {
                context.startActivity(miuiIntentAlt)
                return true
            }
        } catch (_: Exception) {}

        // 2. Delegate across Vivo / Oppo / OnePlus / Samsung / Huawei / Transsion / ASUS / AOSP
        return openOemBackgroundSettings(context)
    }
}
