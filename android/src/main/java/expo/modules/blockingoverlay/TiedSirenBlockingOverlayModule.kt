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
    }
}
