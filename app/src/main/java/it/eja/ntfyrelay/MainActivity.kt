// Copyright (C) 2024 by Ubaldo Porcheddu <ubaldo@eja.it>
// Copyright (C) 2025 by Takase

package it.eja.ntfyrelay

import android.Manifest;
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.core.app.NotificationManagerCompat
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        val enabled = isNotificationListenerEnabled(this)
        if (!enabled) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }

        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        val username = sharedPreferences.getString("USERNAME", "")
        val usernameText = findViewById<EditText>(R.id.usernameText)
        usernameText.setText(username)
        usernameText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val editor = sharedPreferences.edit()
                editor.putString("USERNAME", s.toString())
                editor.apply()
            }
        })

        val password = sharedPreferences.getString("PASSWORD", "")
        val passwordText = findViewById<EditText>(R.id.passwordText)
        passwordText.setText(password)
        passwordText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val editor = sharedPreferences.edit()
                editor.putString("PASSWORD", s.toString())
                editor.apply()
            }
        })

        sharedPreferences.getString("AUTH_TYPE", null)?.let {
            val currentAuthType = NotificationServerConfig.AuthType.valueOf(it)
            setAuthCredentials(sharedPreferences, currentAuthType, true)
        }
        val authTypeGroup = findViewById<RadioGroup>(R.id.authModeGroup)
        authTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAuthNone -> NotificationServerConfig.AuthType.HTTP_NONE
                R.id.radioAuthBasic -> NotificationServerConfig.AuthType.HTTP_BASIC
                R.id.radioAuthToken -> NotificationServerConfig.AuthType.HTTP_TOKEN
                else -> null
            }?.let {
                setAuthCredentials(sharedPreferences, it, false)
                val editor = sharedPreferences.edit()
                editor.putString("AUTH_TYPE", it.toString())
                editor.apply()
            }
        }

        val url = sharedPreferences.getString("URL", "")
        val urlInput = findViewById<EditText>(R.id.url_input)
        urlInput.setText(url)
        urlInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val editor = sharedPreferences.edit()
                editor.putString("URL", s.toString().trim())
                editor.apply()
            }
        })

        val active = sharedPreferences.getBoolean("ACTIVE", false)
        val switchButton = findViewById<ToggleButton>(R.id.startSwitch)
        switchButton.isChecked = active
        switchButton.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("ACTIVE", isChecked)
            editor.apply()
        }

        sharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, s ->
                if (s == "ACTIVE")
                    switchButton.isChecked = sharedPreferences.getBoolean("ACTIVE", false)
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    private fun setAuthCredentials(
        sharedPreferences: SharedPreferences,
        authType: NotificationServerConfig.AuthType,
        setRadio: Boolean
    ) {
        val authTypeGroup = if (setRadio) findViewById<RadioGroup>(R.id.authModeGroup) else null
        val credentialsGroup = findViewById<LinearLayout>(R.id.credentialsLayout)
        val usernameText = findViewById<EditText>(R.id.usernameText)
        val passwordText = findViewById<EditText>(R.id.passwordText)

        when (authType) {
            NotificationServerConfig.AuthType.HTTP_NONE -> {
                authTypeGroup?.check(R.id.radioAuthNone)
                credentialsGroup.visibility = View.GONE
                usernameText.visibility = View.GONE
                passwordText.visibility = View.GONE
            }

            NotificationServerConfig.AuthType.HTTP_BASIC -> {
                authTypeGroup?.check(R.id.radioAuthBasic)
                credentialsGroup.visibility = View.VISIBLE
                usernameText.visibility = View.VISIBLE
                passwordText.visibility = View.VISIBLE
            }

            NotificationServerConfig.AuthType.HTTP_TOKEN -> {
                authTypeGroup?.check(R.id.radioAuthToken)
                credentialsGroup.visibility = View.VISIBLE
                usernameText.visibility = View.GONE
                passwordText.visibility = View.VISIBLE
            }
        }

        usernameText.setText(sharedPreferences.getString("USERNAME", "") ?: "")
        passwordText.setText(sharedPreferences.getString("PASSWORD", "") ?: "")
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(packageName)
    }
}