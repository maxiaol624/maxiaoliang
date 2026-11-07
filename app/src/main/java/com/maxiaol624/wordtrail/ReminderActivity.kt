package com.maxiaol624.wordtrail

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.maxiaol624.wordtrail.databinding.ActivityReminderBinding
import com.maxiaol624.wordtrail.reminder.ReminderLoad
import com.maxiaol624.wordtrail.reminder.ReminderScreen
import com.maxiaol624.wordtrail.reminder.ReminderViewModel
import com.maxiaol624.wordtrail.reminder.ReviewNotifications
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class ReminderActivity : AppCompatActivity() {
    private val model by lazy { ViewModelProvider(this)[ReminderViewModel::class.java] }
    private lateinit var binding: ActivityReminderBinding
    private var latest: ReminderScreen? = null
    private var notificationsAllowed: Boolean? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = ReviewNotifications.allowed(this)
        model.permissionResult(granted)
        latest?.let(::render)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        binding.back.setOnClickListener { finish() }
        binding.retry.setOnClickListener { model.retry() }
        binding.openBooks.setOnClickListener { startActivity(Intent(this, CatalogActivity::class.java)) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { model.state.collect(::render) }
        }
    }

    override fun onResume() {
        super.onResume()
        val allowed = ReviewNotifications.allowed(this)
        val restored = notificationsAllowed == false && allowed
        notificationsAllowed = allowed
        if (restored) model.permissionRestored()
        latest?.let(::render)
    }

    private fun render(screen: ReminderScreen) {
        latest = screen
        val settings = (screen.load as? ReminderLoad.Ready)?.settings
        binding.reminderControls.isVisible = settings != null
        binding.reminderMessage.isVisible = settings == null || screen.saveFailed || screen.saving
        binding.retry.isVisible = screen.load is ReminderLoad.Error || screen.saveFailed
        binding.retry.isEnabled = !screen.saving
        binding.reminderMessage.setText(when {
            screen.load is ReminderLoad.Error -> R.string.reminder_read_failed
            screen.saveFailed -> R.string.reminder_save_failed
            screen.saving -> R.string.reminder_saving
            else -> R.string.catalog_loading
        })
        if (settings == null) return

        val allowed = notificationsAllowed ?: ReviewNotifications.allowed(this)
        binding.reminderEnabled.setOnCheckedChangeListener(null)
        binding.reminderEnabled.isChecked = settings.enabled
        binding.reminderEnabled.isEnabled = !screen.saving
        binding.reminderEnabled.setOnCheckedChangeListener { _, enabled -> model.setEnabled(enabled) }
        binding.reminderTime.isEnabled = !screen.saving
        binding.reminderTime.text = getString(R.string.reminder_time, settings.time.format(DateTimeFormatter.ofPattern("HH:mm")))
        binding.reminderTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute -> model.setTime(LocalTime.of(hour, minute)) },
                settings.time.hour, settings.time.minute, true).show()
        }
        binding.reminderStatus.setText(when {
            !settings.enabled -> R.string.reminder_off
            allowed -> R.string.reminder_on
            else -> R.string.reminder_blocked
        })
        binding.notificationPermission.isVisible = settings.enabled && !allowed
        binding.notificationPermission.isEnabled = !screen.saving
        val request = Build.VERSION.SDK_INT >= 33 && !ReviewNotifications.hasPermission(this) && !settings.permissionAsked
        binding.notificationPermission.setText(if (request) R.string.reminder_allow else R.string.reminder_system_settings)
        binding.notificationPermission.setOnClickListener {
            if (request && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else startActivity(ReviewNotifications.settingsIntent(this))
        }
    }
}
