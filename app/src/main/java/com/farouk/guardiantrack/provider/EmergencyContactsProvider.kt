package com.farouk.guardiantrack.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.room.Room
import com.farouk.guardiantrack.data.local.GuardianDatabase
import com.farouk.guardiantrack.data.local.entity.EmergencyContactEntity

/**
 * ContentProvider exposing emergency contacts to other applications.
 *
 * URI: content://com.guardian.track.provider/emergency_contacts
 * Columns: _id (INTEGER), name (TEXT), phone_number (TEXT)
 *
 * Security: Protected by custom signature|privileged permission
 * (com.guardian.track.READ_EMERGENCY_CONTACTS) declared in the manifest.
 */
class EmergencyContactsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.guardian.track.provider"
        const val PATH_CONTACTS = "emergency_contacts"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CONTACTS")

        private const val CONTACTS = 1
        private const val CONTACT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_CONTACTS, CONTACTS)
            addURI(AUTHORITY, "$PATH_CONTACTS/#", CONTACT_ID)
        }
    }

    private lateinit var database: GuardianDatabase

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            database = Room.databaseBuilder(
                ctx.applicationContext,
                GuardianDatabase::class.java,
                GuardianDatabase.DATABASE_NAME
            ).build()
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val cursor = when (uriMatcher.match(uri)) {
            CONTACTS -> database.emergencyContactDao().getAllContactsCursor()
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                database.emergencyContactDao().getContactByIdCursor(id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CONTACTS) {
            throw IllegalArgumentException("Invalid URI for insert: $uri")
        }

        values ?: return null
        val name = values.getAsString("name") ?: return null
        val phoneNumber = values.getAsString("phone_number") ?: return null

        val entity = EmergencyContactEntity(name = name, phoneNumber = phoneNumber)

        // Use a thread to insert since ContentProvider methods run on binder thread
        val id = kotlinx.coroutines.runBlocking {
            database.emergencyContactDao().insertContact(entity)
        }

        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return when (uriMatcher.match(uri)) {
            CONTACT_ID -> {
                val id = ContentUris.parseId(uri)
                kotlinx.coroutines.runBlocking {
                    database.emergencyContactDao().deleteContact(id)
                }
                context?.contentResolver?.notifyChange(uri, null)
                1
            }
            else -> throw IllegalArgumentException("Invalid URI for delete: $uri")
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        // Update not required by spec, but return 0 for completeness
        return 0
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            CONTACTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_CONTACTS"
            CONTACT_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_CONTACTS"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}
