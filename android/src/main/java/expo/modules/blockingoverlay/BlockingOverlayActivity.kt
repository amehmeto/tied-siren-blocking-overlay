package expo.modules.blockingoverlay

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class BlockingOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_BLOCK_UNTIL = "blockUntil"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocking_overlay)

        val closeButton = findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            goToHomeScreen()
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - prevent back button from dismissing the overlay
    }
}
