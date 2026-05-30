package com.activitypoints.data.api

import com.activitypoints.models.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * The backend returns some fields as either a full object OR a plain string ID,
 * depending on which endpoint is called:
 *
 *   GET /certificates/my        → certificate.student = "someStringId"  (NOT populated)
 *   GET /tutors/certificates    → certificate.student = { _id, name, … } (populated)
 *   GET /tutors/certificates/pending → same, populated
 *
 * A standard Gson data class can't handle this polymorphism — it throws:
 *   Expected BEGIN_OBJECT but was STRING
 *
 * These custom deserializers silently handle both cases.
 */

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
            // KEY FIX: student can be a string ID or a full Student object — handle both
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

private fun JsonObject.getStringOrEmpty(key: String, default: String = ""): String {
    val el = get(key) ?: return default
    return if (el.isJsonNull || (el.isJsonPrimitive && el.asString.isNullOrEmpty())) default
    else if (el.isJsonPrimitive) el.asString else default
}

private fun JsonObject.getStringOrNull(key: String): String? {
    val el = get(key) ?: return null
    return if (el.isJsonNull || !el.isJsonPrimitive) null else el.asString.ifEmpty { null }
}

private fun JsonObject.getIntOrNull(key: String): Int? {
    val el = get(key) ?: return null
    return if (el.isJsonNull || !el.isJsonPrimitive) null
    else runCatching { el.asInt }.getOrNull()
}

/**
 * Deserialize a field that might be:
 *   - absent / null → return null
 *   - a plain string (student ID from unpopulated endpoint) → return null (no name info)
 *   - a JSON object (populated student) → deserialize normally
 */
private fun JsonObject.getStudentOrNull(key: String, context: JsonDeserializationContext): Student? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonPrimitive) return null   // just an ID string — no usable data, return null
    return runCatching { context.deserialize<Student>(el, Student::class.java) }.getOrNull()
}

/**
 * Deserialize a field that might be a string (category ID) or a Category object.
 */
private fun JsonObject.getObjectOrNull(key: String, context: JsonDeserializationContext): Category? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonPrimitive) return null   // just an ID string — no name data available
    return runCatching { context.deserialize<Category>(el, Category::class.java) }.getOrNull()
}
