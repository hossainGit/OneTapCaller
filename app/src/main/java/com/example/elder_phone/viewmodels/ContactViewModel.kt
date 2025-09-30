package com.example.elder_phone.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elder_phone.data.Contact
import com.example.elder_phone.data.ContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactViewModel(private val repository: ContactRepository) : ViewModel() {

    val contacts = repository.getAllContacts()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insertContact(contact: Contact) = viewModelScope.launch {
        repository.insertContact(contact)
    }

    fun updateContact(contact: Contact) = viewModelScope.launch {
        repository.updateContact(contact)
    }

    fun deleteContact(contact: Contact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    suspend fun getContact(contactId: Int): Contact? =
        repository.getContactById(contactId)
}