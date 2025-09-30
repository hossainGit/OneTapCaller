package com.example.elder_phone.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.elder_phone.R
import com.example.elder_phone.adapters.ContactAdapter
import com.example.elder_phone.data.Contact
import com.example.elder_phone.data.ContactDatabase
import com.example.elder_phone.data.ContactRepository
import com.example.elder_phone.viewmodels.ContactViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ContactViewModel
    private lateinit var adapter: ContactAdapter

    // Permission launcher for phone calls
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCallNumber != null) {
            makeCall(pendingCallNumber!!)
            pendingCallNumber = null
        }
    }

    private var pendingCallNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupViewModel() {
        val database = ContactDatabase.getInstance(this)
        val repository = ContactRepository(database.contactDao())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactViewModel(repository) as T
            }
        })[ContactViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(
            onContactClick = { contact: Contact ->
                pendingCallNumber = contact.phone
                requestCallPermission()
            },
            isEditMode = false
        )

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns grid

        // Observe contacts StateFlow using lifecycleScope
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contacts.collect { contacts ->
                    adapter.submitList(contacts)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                startActivity(Intent(this, EditModeActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        // FAB for adding new contact
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, AddContactActivity::class.java))
        }

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    private fun requestCallPermission() {
        requestPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
    }

    private fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }
}
