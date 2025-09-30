package com.example.elder_phone.data

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {

    fun getAllContacts(): Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun getContactById(contactId: Int): Contact? =
        contactDao.getContactById(contactId)

    suspend fun insertContact(contact: Contact): Long =
        contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) =
        contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) =
        contactDao.deleteContact(contact)

    suspend fun deleteContactById(contactId: Int) =
        contactDao.deleteContactById(contactId)
}