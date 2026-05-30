package com.activitypoints.data.api

import com.activitypoints.models.*
import com.google.gson.*
import java.lang.reflect.Type

/**
 * The backend returns some fields as either a full object OR a plain string ID,
 * depending on which endpoint is called.
 *
 * Additionally, GET /students/me populates `batch` and `branch` as objects
 * { _id, name } while the login response returns only { name } for the student.
 * A standard Gson data class can't handle this polymorphism.
 *
 * These custom deserializers silently handle both cases.
 */

// ── Student ────────────────────────────────────────────────────────────────────

class StudentDeserializer : JsonDeserializer<Student> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Student {
        if (json.isJsonNull) return Student()
        val obj = json.asJsonObject
        return Student(
            id             = obj.getStringOrEmpty("_id"),
            name           = obj.getStringOrEmpty("name"),
            registerNumber = obj.getStringOrEmpty("registerNumber"),
            email          = obj.getStringOrEmpty("email"),
            isLateralEntry = obj.getBoolOrDefault("isLateralEntry", false),
            // batch and branch can be a populated object { _id, name } or just an ID string
            batchName      = obj.getNestedNameOrNull("batch"),
            branchName     = obj.getNestedNameOrNull("branch"),
            photoUrl       = obj.getStringOrNull("profilePhoto"),
            phone          = obj.getStringOrNull("phone"),
            semester       = obj.getStringOrNull("semester"),
        )
    }
}

// ── Certificate ────────────────────────────────────────────────────────────────

class CertificateDeserializer : JsonDeserializer<Certificate> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Certificate {
        val obj = json.asJsonObject
        return Certificate(
            id              = obj.getStringOrEmpty("_id"),
            eventName       = obj.getStringOrNull("eventName"),
            subcategory     = obj.getStringOrNull("subcategory"),
            category        = obj.getObjectOrNull("category", context),
            level           = obj.getStringOrNull("level"),
            prizeType       = obj.getStringOrNull("prizeType"),
            status          = obj.getStringOrEmpty("status", "pending"),
            pointsAwarded   = obj.getIntOrNull("pointsAwarded"),
            potentialPoints = obj.getIntOrNull("potentialPoints"),
            fileUrl         = obj.getStringOrNull("fileUrl"),
            eventDate       = obj.getStringOrNull("eventDate"),
            uploadedAt      = obj.getStringOrNull("uploadedAt"),
            rejectionReason = obj.getStringOrNull("rejectionReason"),
            remarks         = obj.getStringOrNull("remarks"),
            student         = obj.getStudentOrNull("student", context),
        )
    }
}

// ── TutorPendingCert ───────────────────────────────────────────────────────────

class TutorPendingCertDeserializer : JsonDeserializer<TutorPendingCert> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): TutorPendingCert {
        val obj = json.asJsonObject
        return TutorPendingCert(
            id              = obj.getStringOrEmpty("_id"),
            eventName       = obj.getStringOrNull("eventName"),
            subcategory     = obj.getStringOrNull("subcategory"),
            category        = obj.getObjectOrNull("category", context),
            level           = obj.getStringOrNull("level"),
            prizeType       = obj.getStringOrNull("prizeType"),
            status          = obj.getStringOrEmpty("status", "pending"),
            potentialPoints = obj.getIntOrNull("potentialPoints"),
            fileUrl         = obj.getStringOrNull("fileUrl"),
            eventDate       = obj.getStringOrNull("eventDate"),
            student         = obj.getStudentOrNull("student", context),
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

internal fun JsonObject.getStringOrEmpty(key: String, default: String = ""): String {
    val el = get(key) ?: return default
    return if (el.isJsonNull || !el.isJsonPrimitive) default
    else el.asString.ifEmpty { default }
}

internal fun JsonObject.getStringOrNull(key: String): String? {
    val el = get(key) ?: return null
    return if (el.isJsonNull || !el.isJsonPrimitive) null else el.asString.ifEmpty { null }
}

internal fun JsonObject.getIntOrNull(key: String): Int? {
    val el = get(key) ?: return null
    return if (el.isJsonNull || !el.isJsonPrimitive) null
    else runCatching { el.asInt }.getOrNull()
}

internal fun JsonObject.getBoolOrDefault(key: String, default: Boolean): Boolean {
    val el = get(key) ?: return default
    return if (el.isJsonNull || !el.isJsonPrimitive) default
    else runCatching { el.asBoolean }.getOrElse { default }
}

/**
 * For fields like `batch` / `branch` that the backend populates as { _id, name }.
 * Returns the `name` string, or null if the field is missing / a plain ID string.
 */
internal fun JsonObject.getNestedNameOrNull(key: String): String? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonPrimitive) return null  // plain ObjectId string — no name available
    return runCatching { el.asJsonObject.getStringOrNull("name") }.getOrNull()
}

/**
 * Deserialize a field that might be a plain string ID or a full Student object.
 */
internal fun JsonObject.getStudentOrNull(key: String, context: JsonDeserializationContext): Student? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonPrimitive) return null  // just an ID string
    return runCatching { context.deserialize<Student>(el, Student::class.java) }.getOrNull()
}

/**
 * Deserialize a field that might be a string ID or a Category object.
 */
internal fun JsonObject.getObjectOrNull(key: String, context: JsonDeserializationContext): Category? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonPrimitive) return null  // just an ID string
    return runCatching { context.deserialize<Category>(el, Category::class.java) }.getOrNull()
}
