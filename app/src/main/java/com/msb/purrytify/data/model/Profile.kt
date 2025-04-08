package com.msb.purrytify.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OffsetDateTimeAdapter : JsonAdapter<OffsetDateTime>() {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun fromJson(reader: JsonReader): OffsetDateTime? {
        return try {
            OffsetDateTime.parse(reader.nextString(), formatter)
        } catch (e: Exception) {
            null // Or handle the error as needed
        }
    }

    override fun toJson(writer: JsonWriter, value: OffsetDateTime?) {
        writer.value(value?.format(formatter))
    }
}

@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "id")
    val id: Int, // Assuming ID is an Integer based on your initial format
    @Json(name = "username")
    val username: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "profilePhoto")
    val profilePhoto: String = "dummy.png",
    @Json(name = "location")
    val location: String,
    @Json(name = "createdAt")
    val createdAtString: String,
    @Json(name = "updatedAt")
    val updatedAtString: String,
    val addedSongsCount: Int = 0,
    val likedSongsCount: Int = 0,
    val listenedSongsCount: Int = 0
) {
    val profilePhotoUrl: String
        get() = "http://34.101.226.132:3000/uploads/profile-picture/$profilePhoto"

    val createdAt: OffsetDateTime
        get() = OffsetDateTime.parse(createdAtString)

    val updatedAt: OffsetDateTime
        get() = OffsetDateTime.parse(updatedAtString)

    val formattedCreatedAt: String
        get() = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    val formattedUpdatedAt: String
        get() = updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

data class ProfileError(val error: String)