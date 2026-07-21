package com.lifeos.personal.data.google

import android.content.Context
import android.content.Intent
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleSyncResult(val folderId: String, val calendarId: String, val message: String)

class GoogleSyncRepository(context: Context) {
    private val credential = GoogleAccountCredential.usingOAuth2(
        context,
        listOf(DriveScopes.DRIVE_FILE, CalendarScopes.CALENDAR),
    )

    var accountName: String?
        get() = credential.selectedAccountName
        set(value) { credential.selectedAccountName = value }

    fun chooseAccountIntent(): Intent = credential.newChooseAccountIntent()

    suspend fun sync(summaryJson: String): GoogleSyncResult = withContext(Dispatchers.IO) {
        check(!credential.selectedAccountName.isNullOrBlank()) { "Сначала выберите Google-аккаунт" }
        val transport = AndroidHttp.newCompatibleTransport()
        val json = GsonFactory.getDefaultInstance()
        val drive = Drive.Builder(transport, json, credential).setApplicationName("LifeOS").build()
        val calendar = Calendar.Builder(transport, json, credential).setApplicationName("LifeOS").build()

        val folderId = findOrCreateFolder(drive)
        val calendarId = findOrCreateCalendar(calendar)
        uploadState(drive, folderId, summaryJson, calendarId)
        GoogleSyncResult(folderId, calendarId, "Данные сохранены в LifeOS на Google Drive")
    }

    private fun findOrCreateFolder(drive: Drive): String {
        val found = drive.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and appProperties has { key='lifeosManaged' and value='true' }")
            .setSpaces("drive").setFields("files(id,name)").execute().files.firstOrNull()
        if (found != null) return found.id
        return drive.files().create(
            File().setName("LifeOS").setMimeType("application/vnd.google-apps.folder")
                .setAppProperties(mapOf("lifeosManaged" to "true")),
        ).setFields("id").execute().id
    }

    private fun findOrCreateCalendar(api: Calendar): String {
        val found: CalendarListEntry? = api.calendarList().list().execute().items
            .firstOrNull { it.summary == "LifeOS" }
        if (found != null) return found.id
        return api.calendars().insert(
            com.google.api.services.calendar.model.Calendar()
                .setSummary("LifeOS")
                .setDescription("Отдельный план дня LifeOS. Основной календарь не изменяется."),
        ).execute().id
    }

    private fun uploadState(drive: Drive, folderId: String, summaryJson: String, calendarId: String) {
        val content = summaryJson.dropLastWhile { it.isWhitespace() }.removeSuffix("}") +
            ",\n  \"calendarId\": \"$calendarId\"\n}"
        val existing = drive.files().list()
            .setQ("name='lifeos_config.json' and '$folderId' in parents and trashed=false")
            .setSpaces("drive").setFields("files(id)").execute().files.firstOrNull()
        val media = ByteArrayContent.fromString("application/json; charset=utf-8", content)
        if (existing == null) {
            drive.files().create(File().setName("lifeos_config.json").setParents(listOf(folderId)), media)
                .setFields("id").execute()
        } else {
            drive.files().update(existing.id, File(), media).execute()
        }
    }
}
