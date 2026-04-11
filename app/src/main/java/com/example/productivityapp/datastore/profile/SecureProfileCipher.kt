package com.example.productivityapp.datastore.profile

/**
 * Slice-1 seam for future encryption-at-rest behavior.
 *
 * For now this is a transform hook around the typed record, allowing the secure store and migration
 * coordinator to be tested without changing current repository wiring or committing to the final
 * field/blob encryption strategy yet.
 */
interface SecureProfileCipher {
    fun beforeWrite(record: SecureStoredUserProfile): SecureStoredUserProfile
    fun afterRead(record: SecureStoredUserProfile): SecureStoredUserProfile
}

object PassthroughSecureProfileCipher : SecureProfileCipher {
    override fun beforeWrite(record: SecureStoredUserProfile): SecureStoredUserProfile = record

    override fun afterRead(record: SecureStoredUserProfile): SecureStoredUserProfile = record
}

