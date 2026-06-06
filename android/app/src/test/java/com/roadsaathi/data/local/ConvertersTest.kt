package com.roadsaathi.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromSyncStatus returns uppercase name`() {
        assertThat(converters.fromSyncStatus(SyncStatus.PENDING)).isEqualTo("PENDING")
        assertThat(converters.fromSyncStatus(SyncStatus.SYNCED)).isEqualTo("SYNCED")
        assertThat(converters.fromSyncStatus(SyncStatus.FAILED)).isEqualTo("FAILED")
    }

    @Test
    fun `toSyncStatus returns matching enum`() {
        assertThat(converters.toSyncStatus("PENDING")).isEqualTo(SyncStatus.PENDING)
        assertThat(converters.toSyncStatus("SYNCED")).isEqualTo(SyncStatus.SYNCED)
        assertThat(converters.toSyncStatus("FAILED")).isEqualTo(SyncStatus.FAILED)
    }

    @Test
    fun `toSyncStatus returns PENDING for unknown values`() {
        assertThat(converters.toSyncStatus("UNKNOWN")).isEqualTo(SyncStatus.PENDING)
        assertThat(converters.toSyncStatus("")).isEqualTo(SyncStatus.PENDING)
    }
}
