package com.example.productivityapp.datastore.profile

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.DataStoreFactory
import com.example.productivityapp.datastore.profile.proto.UserProfileProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class EncryptedProtoUserProfileStore(
    private val dataStore: DataStore<UserProfileProto>,
    private val cipher: SecureProfileCipher = PassthroughSecureProfileCipher,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : SecureUserProfileStore {

    override fun observe(): Flow<SecureStoredUserProfile> = dataStore.data.map { proto ->
        cipher.afterRead(UserProfileSchemaMapper.fromProto(proto))
    }

    override suspend fun read(): SecureStoredUserProfile = observe().first()

    override suspend fun write(record: SecureStoredUserProfile) {
        val transformed = cipher.beforeWrite(
            record.copy(lastWriteEpochMs = clock())
        )
        dataStore.updateData {
            UserProfileSchemaMapper.toProto(transformed)
        }
    }

    override suspend fun clear() {
        dataStore.updateData {
            ProtoUserProfileSerializer.defaultValue
        }
    }

    companion object {
        fun create(file: File): EncryptedProtoUserProfileStore {
            val scope = CoroutineScope(Dispatchers.IO + Job())
            val dataStore = DataStoreFactory.create(
                serializer = ProtoUserProfileSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { ProtoUserProfileSerializer.defaultValue },
                scope = scope,
                produceFile = { file },
            )
            return EncryptedProtoUserProfileStore(dataStore = dataStore)
        }
    }
}

