package com.roadsaathi.data.local

import com.roadsaathi.data.local.entity.HazardReportEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalHazardReportTest {

    @Test
    fun `toLocalReport maps all fields correctly`() {
        val entity = HazardReportEntity(
            localId = "test-id",
            hazardType = "POTHOLE",
            latitude = 28.6139,
            longitude = 77.2090,
            photoLocalPath = "/path/to/photo.jpg",
            classificationLabel = "pothole",
            confidenceScore = 0.85f,
            reportedAt = 1000L,
            syncStatus = SyncStatus.PENDING,
            remoteId = null,
            nhCorridor = "NH-44",
            severity = 2
        )

        val local = entity.toLocalReport()

        assertThat(local.localId).isEqualTo("test-id")
        assertThat(local.hazardType).isEqualTo("POTHOLE")
        assertThat(local.latitude).isEqualTo(28.6139)
        assertThat(local.longitude).isEqualTo(77.2090)
        assertThat(local.photoLocalPath).isEqualTo("/path/to/photo.jpg")
        assertThat(local.classificationLabel).isEqualTo("pothole")
        assertThat(local.confidenceScore).isEqualTo(0.85f)
        assertThat(local.reportedAt).isEqualTo(1000L)
        assertThat(local.nhCorridor).isEqualTo("NH-44")
        assertThat(local.severity).isEqualTo(2)
    }
}
