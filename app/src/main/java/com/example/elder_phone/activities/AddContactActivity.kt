package com.example.elder_phone.activities

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.elder_phone.R
import com.example.elder_phone.data.Contact
import com.example.elder_phone.data.ContactRepository
import com.example.elder_phone.data.ContactDatabase
import com.example.elder_phone.viewmodels.ContactViewModel
import com.example.elder_phone.utils.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File

class AddContactActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var contactImage: ShapeableImageView

    private var selectedImageUri: Uri? = null
    private lateinit var viewModel: ContactViewModel

    // Image picker launchers
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { originalUri ->
            lifecycleScope.launch {
                val compressedUri = ImageUtils.compressAndResizeImage(
                    context = this@AddContactActivity,
                    originalUri = originalUri,
                    maxWidth = 720,
                    maxHeight = 720
                )
                if (compressedUri != null) {
                    selectedImageUri = compressedUri
                    contactImage.load(compressedUri)
                } else {
                    selectedImageUri = originalUri
                    contactImage.load(originalUri)
                }
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            contactImage.load(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        setupToolbar()
        setupViewModel()
        setupViews()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
                @Suppress("UNCHECKED_CAST")
                return ContactViewModel(repository) as T
            }
        })[ContactViewModel::class.java]
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
        findViewById<MaterialButton>(R.id.btn_capture).setOnClickListener {
            selectedImageUri = createImageFileUri()
            selectedImageUri?.let {
                takePictureLauncher.launch(it)
            }
        }

        // Save button
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            saveContact()
        }

        // Back button
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun createImageFileUri(): Uri {
        val file = File.createTempFile(
            "contact_photo_${System.currentTimeMillis()}",
            ".jpg",
            externalCacheDir
        )
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun saveContact() {
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val contact = Contact(
            name = name,
            phone = phone,
            photoUri = selectedImageUri?.toString()
        )

        viewModel.insertContact(contact)
        Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
