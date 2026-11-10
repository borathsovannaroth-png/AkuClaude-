package com.akula.akuclaude

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuBridge {

    enum class Status { CONNECTED, NOT_CONNECTED, NOT_INSTALLED }

    fun checkStatus(context: Context): Status {
        val installed = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (!installed) return Status.NOT_INSTALLED

        return try {
            if (!Shizuku.pingBinder()) {
                Status.NOT_CONNECTED
            } else {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (granted) Status.CONNECTED else Status.NOT_CONNECTED
            }
        } catch (e: Throwable) {
            Status.NOT_CONNECTED
        }
    }

    fun requestPermission(requestCode: Int) {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Throwable) {
            // Shizuku service not running; status stays NOT_CONNECTED
        }
    }
}
