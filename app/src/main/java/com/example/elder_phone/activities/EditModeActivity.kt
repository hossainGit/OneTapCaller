package com.example.elder_phone.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.example.elder_phone.R
import com.example.elder_phone.data.Contact
import com.example.elder_phone.data.ContactRepository
import com.example.elder_phone.data.ContactDatabase
import com.example.elder_phone.viewmodels.ContactViewModel
import com.example.elder_phone.adapters.ContactAdapter
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch

class EditModeActivity : AppCompatActivity() {

    private lateinit var viewModel: ContactViewModel
    private lateinit var adapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_mode)
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
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
                // Open EditContactActivity when contact is tapped
                val intent = Intent(this, EditContactActivity::class.java).apply {
                    putExtra("CONTACT_ID", contact.id)
                }
                startActivity(intent)
            },
            isEditMode = true
        )

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Observe contacts StateFlow using lifecycleScope
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contacts.collect { contacts ->
                    adapter.submitList(contacts)
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Contacts"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}