package com.activitypoints.utils

import com.activitypoints.models.Category
import com.activitypoints.models.Certificate

// ── SBTE Kerala Activity Points Rules ─────────────────────────────────────────
// Exact port of src/utils/calcPoints.ts

object CalcPoints {

    private const val PASS_REGULAR  = 60
    private const val PASS_LATERAL  = 40
    private const val CAP_REGULAR   = 40
    private const val CAP_LATERAL   = 30
    private const val DEFAULT_MAX   = 40

    fun calcCappedPoints(
        approvedCerts: List<Certificate>,
        categories: List<Category> = emptyList(),
        isLateralEntry: Boolean = false,
    ): Int {
        val perSegmentCap = if (isLateralEntry) CAP_LATERAL else CAP_REGULAR

        // Group certs by category ID
        data class Group(val certs: MutableList<Certificate>, val catDoc: Category?)
        val grouped = mutableMapOf<String, Group>()

        for (cert in approvedCerts) {
            val catId = cert.category?.id ?: continue
            if (catId !in grouped) {
                val catDoc = cert.category.takeIf { it.name.isNotBlank() }
                    ?: categories.find { it.id == catId }
                grouped[catId] = Group(mutableListOf(), catDoc)
            }
            grouped[catId]!!.certs.add(cert)
        }

        var grandTotal = 0

        for ((_, group) in grouped) {
            val (certs, catDoc) = group
            val catName         = catDoc?.name?.lowercase() ?: ""
            val catMaxPts       = catDoc?.maxPoints ?: DEFAULT_MAX
            val hasExplicitCeil = catMaxPts != DEFAULT_MAX
            val effectiveCap    = when {
                hasExplicitCeil && isLateralEntry -> minOf(catMaxPts, perSegmentCap)
                hasExplicitCeil                   -> catMaxPts
                else                              -> perSegmentCap
            }

            val catSum = if (catName.contains("arts") ||
                             catName.contains("sports") ||
                             catName.contains("games")) {
                certs.maxOfOrNull { it.pointsAwarded ?: 0 } ?: 0
            } else {
                certs.sumOf { it.pointsAwarded ?: 0 }
            }

            grandTotal += minOf(catSum, effectiveCap)
        }

        return grandTotal
    }

    fun passThreshold(isLateralEntry: Boolean = false): Int =
        if (isLateralEntry) PASS_LATERAL else PASS_REGULAR
}
