package com.example.elder_phone.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.example.elder_phone.R
import com.example.elder_phone.data.Contact
import com.example.elder_phone.data.ContactRepository
import com.example.elder_phone.data.ContactDatabase
import com.example.elder_phone.viewmodels.ContactViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.runBlocking

class EditContactActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var contactImage: ShapeableImageView

    private var selectedImageUri: Uri? = null
    private var currentContact: Contact? = null
    private lateinit var viewModel: ContactViewModel

    // Image picker launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { originalUri: Uri ->
            selectedImageUri = originalUri
            contactImage.load(originalUri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && selectedImageUri != null) {
            contactImage.load(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)
        setupToolbar()
        setupViewModel()
        setupViews()
        loadContactData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Contact"
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
    private fun setupViewModel() {
        val database = ContactDatabase.getInstance(this)
        val repository = ContactRepository(database.contactDao())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactViewModel(repository) as T
            }
        })[ContactViewModel::class.java]
    }

    private fun loadContactData() {
        val contactId = intent.getIntExtra("CONTACT_ID", -1)
        if (contactId != -1) {
            runBlocking {
                currentContact = viewModel.getContact(contactId)
                currentContact?.let { contact ->
                    nameEditText.setText(contact.name)
                    phoneEditText.setText(contact.phone)
                    contact.photoUri?.let { uriString ->
                        selectedImageUri = Uri.parse(uriString)
                        contactImage.load(selectedImageUri)
                    }
                }
            }
        }
    }

    private fun setupViews() {
        nameEditText = findViewById(R.id.et_name)
        phoneEditText = findViewById(R.id.et_phone)
        contactImage = findViewById(R.id.iv_contact)

        // Upload photo button
        findViewById<MaterialButton>(R.id.btn_upload).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Capture photo button
        // Capture photo button
        findViewById<MaterialButton>(R.id.btn_capture).setOnClickListener {
            // Create a new non-nullable Uri and store it in a local variable
            val imageUri = createImageFileUri()

            // Assign it to your class property
            selectedImageUri = imageUri

            // Launch the camera with the guaranteed non-null local variable
            takePictureLauncher.launch(imageUri)
        }

        // Save button
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            updateContact()
        }

        // Delete button
        findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            showDeleteConfirmation()
        }

        // Back button
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun createImageFileUri(): Uri {
        val file = java.io.File.createTempFile(
            "contact_photo_${System.currentTimeMillis()}",
            ".jpg",
            externalCacheDir
        )
        return androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun updateContact() {
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        currentContact?.let { contact ->
            val updatedContact = contact.copy(
                name = name,
                phone = phone,
                photoUri = selectedImageUri?.toString()
            )

            viewModel.updateContact(updatedContact)
            Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteContactWithUndo()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteContactWithUndo() {
        currentContact?.let { contactToDelete ->
            viewModel.deleteContact(contactToDelete)

            val snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "Contact deleted",
                Snackbar.LENGTH_LONG
            )
            snackbar.setAction("UNDO") {
                // Restore the contact
                viewModel.insertContact(contactToDelete)
                Toast.makeText(this, "Contact restored", Toast.LENGTH_SHORT).show()
            }
            snackbar.show()

            finish()
        }
    }
}