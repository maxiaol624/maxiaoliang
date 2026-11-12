package com.maxiaol624.wordtrail

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.maxiaol624.wordtrail.databinding.ActivityMainBinding
import com.maxiaol624.wordtrail.data.WordDatabase
import com.maxiaol624.wordtrail.reminder.ReminderCoordinator
import com.maxiaol624.wordtrail.widget.StudyWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        binding.openBooks.setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }
        binding.openStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        binding.openReminders.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
        }
        binding.addWidget.setOnClickListener {
            val manager = AppWidgetManager.getInstance(this)
            if (!manager.isRequestPinAppWidgetSupported ||
                !manager.requestPinAppWidget(StudyWidgetProvider.component(this), null, null)
            ) {
                Toast.makeText(this, R.string.widget_add_manually, Toast.LENGTH_LONG).show()
            }
        }
        if (savedInstanceState == null) openWidgetShortcut(intent.action)
    }

    override fun onResume() {
        super.onResume()
        StudyWidgetProvider.requestRefresh(this)
        lifecycleScope.launch {
            try {
                ReminderCoordinator.refresh(applicationContext)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("ReviewReminder", "Unable to restore reminder scheduling", error)
            }
        }
    }

    private fun openWidgetShortcut(action: String?) {
        val review = when (action) {
            StudyWidgetProvider.LearnAction -> false
            StudyWidgetProvider.ReviewAction -> true
            else -> return
        }
        lifecycleScope.launch {
            val fallback = if (review) StatisticsActivity::class.java else CatalogActivity::class.java
            val bookId = try {
                val database = WordDatabase.get(applicationContext)
                if (review) database.words().firstDueReviewBook(System.currentTimeMillis())?.id
                else database.widget().firstBookWithNewWords()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("StudyWidget", "Unable to open study queue", error)
                null
            }
            val next = if (bookId == null) Intent(this@MainActivity, fallback)
            else StudyActivity.intent(this@MainActivity, bookId, review)
            startActivity(next)
        }
    }
}

internal fun Activity.applySystemBarPadding(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, root).isAppearanceLightNavigationBars = true
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}
