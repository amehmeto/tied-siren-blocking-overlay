package expo.modules.blockingoverlay

import android.content.Intent
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.exception.CodedException

class TiedSirenBlockingOverlayModule : Module() {

    companion object {
        private const val TAG = "TiedSirenBlockingOverlay"
    }

    override fun definition() = ModuleDefinition {
        Name("TiedSirenBlockingOverlay")

        // #9: Expose setBlockingSchedule() API to JS
        AsyncFunction("setBlockingSchedule") { windows: List<Map<String, Any>> ->
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            try {
                val blockingWindows = windows.map { windowMap ->
                    @Suppress("UNCHECKED_CAST")
                    BlockingWindow(
                        id = windowMap["id"] as String,
                        startTime = windowMap["startTime"] as String,
                        endTime = windowMap["endTime"] as String,
                        packageNames = (windowMap["packageNames"] as List<String>)
                    )
                }

                // Sentry breadcrumb for JS -> Native call
                val allPackages = blockingWindows.flatMap { it.packageNames }.distinct()
                SentryHelper.addBreadcrumb("module", "setBlockingSchedule called from JS", mapOf(
                    "windowCount" to blockingWindows.size,
                    "totalPackages" to allPackages.size,
                    "packages" to allPackages.joinToString(","),
                    "windows" to blockingWindows.map { "${it.id}:${it.startTime}-${it.endTime}" }.joinToString("; ")
                ))

                BlockingScheduler(context).setSchedule(blockingWindows)
                Log.d(TAG, "setBlockingSchedule: ${blockingWindows.size} windows configured")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid schedule data: ${e.message}")
                SentryHelper.captureException(e)
                throw CodedException("ERR_INVALID_SCHEDULE", "Invalid schedule data: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set schedule: ${e.message}", e)
                SentryHelper.captureException(e)
                throw CodedException("ERR_SCHEDULE_FAILED", "Failed to set schedule: ${e.message}", e)
            }
        }

        AsyncFunction("getBlockingSchedule") {
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            val windows = BlockingScheduleStorage.getSchedule(context)
            windows.map { window ->
                mapOf(
                    "id" to window.id,
                    "startTime" to window.startTime,
                    "endTime" to window.endTime,
                    "packageNames" to window.packageNames
                )
            }
        }

        AsyncFunction("clearBlockingSchedule") {
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            BlockingScheduler(context).clearSchedule()
            Log.d(TAG, "clearBlockingSchedule: schedule cleared")
        }

        AsyncFunction("showOverlay") { packageName: String ->
            if (packageName.isBlank()) {
                Log.e(TAG, "Invalid package name: empty or blank")
                throw CodedException("ERR_INVALID_PACKAGE", "Package name cannot be empty", null)
            }

            try {
                val context = appContext.reactContext ?: throw CodedException(
                    "ERR_OVERLAY_LAUNCH",
                    "React context is not available",
                    null
                )

                val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
                    putExtra(BlockingOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_HISTORY
                }

                context.startActivity(intent)
                Log.d(TAG, "Overlay launched for package: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch overlay: ${e.message}", e)
                throw CodedException("ERR_OVERLAY_LAUNCH", "Failed to launch overlay: ${e.message}", e)
            }
        }

        AsyncFunction("setBlockedApps") { packageNames: List<String> ->
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            BlockedAppsStorage.setBlockedApps(context, packageNames.toSet())
            Log.d(TAG, "setBlockedApps: ${packageNames.size} apps")
        }

        AsyncFunction("getBlockedApps") {
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            BlockedAppsStorage.getBlockedApps(context).toList()
        }

        AsyncFunction("clearBlockedApps") {
            val context = appContext.reactContext
                ?: throw CodedException("ERR_NO_CONTEXT", "Context not available", null)

            BlockedAppsStorage.clearBlockedApps(context)
            Log.d(TAG, "clearBlockedApps called")
        }
    }
}
