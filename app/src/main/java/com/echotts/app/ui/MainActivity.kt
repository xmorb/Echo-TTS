package com.echotts.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.echotts.app.R
import com.echotts.app.api.AlexaRepository
import com.echotts.app.api.models.Device
import com.echotts.app.databinding.ActivityMainBinding
import com.echotts.app.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AlexaRepository

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    private var selectedDevice: Device? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        repository = AlexaRepository(sessionManager)

        setSupportActionBar(binding.toolbar)

        // Redirect to login if not authenticated
        if (!sessionManager.isLoggedIn) {
            navigateToLogin()
            return
        }

        setupUi()
        observeViewModel()

        // Load devices on start
        viewModel.loadDevices()
    }

    private fun setupUi() {
        // Clear button
        binding.btnClear.setOnClickListener {
            binding.etText.setText("")
            binding.etText.requestFocus()
        }

        // Update clear button visibility based on text content
        binding.etText.addTextChangedListener {
            binding.btnClear.isEnabled = it?.isNotEmpty() == true
        }
        binding.btnClear.isEnabled = false

        // Send to selected device
        binding.btnSend.setOnClickListener {
            val text = binding.etText.text.toString()
            val device = selectedDevice
            if (device == null) {
                showToast("Please select an Echo device.")
                return@setOnClickListener
            }
            viewModel.speakOnDevice(text, device)
        }

        // Broadcast to all devices
        binding.btnSendAll.setOnClickListener {
            val text = binding.etText.text.toString()
            viewModel.speakOnAllDevices(text)
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.loadDevices()
        }

        // Device spinner selection
        binding.spinnerDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val devices = viewModel.devices.value ?: return
                selectedDevice = devices.getOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDevice = null
            }
        }
    }

    private fun observeViewModel() {
        viewModel.devices.observe(this) { devices ->
            updateDeviceSpinner(devices)
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSend.isEnabled = false
                    binding.btnSendAll.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    binding.btnSendAll.isEnabled = true
                    showToast(state.message)
                    viewModel.resetState()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    binding.btnSendAll.isEnabled = true
                    showToast(state.message)
                    // If session expired, go back to login
                    if (state.message.contains("log in again", ignoreCase = true)) {
                        sessionManager.clearSession()
                        navigateToLogin()
                    }
                    viewModel.resetState()
                }
                is UiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    binding.btnSendAll.isEnabled = true
                }
            }
        }
    }

    private fun updateDeviceSpinner(devices: List<Device>) {
        val names = if (devices.isEmpty()) {
            listOf(getString(R.string.no_devices_found))
        } else {
            devices.map { it.displayName }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerDevices.adapter = adapter
        selectedDevice = devices.firstOrNull()

        val hasDevices = devices.isNotEmpty()
        binding.btnSend.isEnabled = hasDevices
        binding.btnSendAll.isEnabled = hasDevices
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                sessionManager.clearSession()
                navigateToLogin()
                true
            }
            R.id.action_refresh -> {
                viewModel.loadDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
