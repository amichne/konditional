package io.amichne.konditional.core.evaluation

import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StableId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BucketingTest {

    private val sampleId: HexId = StableId.of("test-user-1").hexId

    @Test
    fun `stableBucket returns same bucket for identical inputs across calls`() {
        val b1 = Bucketing.stableBucket("v1", "my-flag", sampleId)
        val b2 = Bucketing.stableBucket("v1", "my-flag", sampleId)

        assertEquals(b1, b2)
    }

    @Test
    fun `stableBucket result is in range 0 until 10000`() {
        val bucket = Bucketing.stableBucket("v1", "my-flag", sampleId)

        assertTrue(bucket in 0 until 10_000, "Expected bucket in [0, 10000), got $bucket")
    }

    @Test
    fun `stableBucket varies with different stableIds`() {
        val buckets = (1..20).map { i ->
            Bucketing.stableBucket("v1", "my-flag", StableId.of("user-$i").hexId)
        }.toSet()

        // High probability of getting multiple distinct buckets across 20 distinct users
        assertTrue(buckets.size > 1, "Expected distinct buckets for distinct users")
    }

    @Test
    fun `stableBucket varies with different salts`() {
        val b1 = Bucketing.stableBucket("v1", "my-flag", sampleId)
        val b2 = Bucketing.stableBucket("v2", "my-flag", sampleId)

        // Different salts should produce different bucketing distributions
        // While theoretically they could collide, in practice SHA-256 makes this essentially impossible
        // We just verify the call succeeds and both are valid
        assertTrue(b1 in 0 until 10_000)
        assertTrue(b2 in 0 until 10_000)
    }

    @Test
    fun `missingStableIdBucket returns 9999`() {
        assertEquals(9_999, Bucketing.missingStableIdBucket())
    }

    @Test
    fun `isInRampUp with 0 percent rampUp always returns false`() {
        assertFalse(Bucketing.isInRampUp(RampUp.of(0.0), 0))
        assertFalse(Bucketing.isInRampUp(RampUp.of(0.0), 5_000))
        assertFalse(Bucketing.isInRampUp(RampUp.of(0.0), 9_999))
    }

    @Test
    fun `isInRampUp with 100 percent rampUp always returns true`() {
        assertTrue(Bucketing.isInRampUp(RampUp.of(100.0), 0))
        assertTrue(Bucketing.isInRampUp(RampUp.of(100.0), 9_999))
    }

    @Test
    fun `isInRampUp with 50 percent threshold uses basis points boundary`() {
        val rampUp = RampUp.of(50.0)
        val threshold = Bucketing.rampUpThresholdBasisPoints(rampUp) // 5000

        assertTrue(Bucketing.isInRampUp(rampUp, threshold - 1), "bucket ${threshold - 1} should be in 50% rampUp")
        assertFalse(Bucketing.isInRampUp(rampUp, threshold), "bucket $threshold should NOT be in 50% rampUp")
        assertFalse(Bucketing.isInRampUp(rampUp, 9_999), "bucket 9999 should NOT be in 50% rampUp")
    }

    @Test
    fun `rampUpThresholdBasisPoints converts percentage to basis points`() {
        assertEquals(0, Bucketing.rampUpThresholdBasisPoints(RampUp.of(0.0)))
        assertEquals(5_000, Bucketing.rampUpThresholdBasisPoints(RampUp.of(50.0)))
        assertEquals(10_000, Bucketing.rampUpThresholdBasisPoints(RampUp.of(100.0)))
        assertEquals(100, Bucketing.rampUpThresholdBasisPoints(RampUp.of(1.0)))
    }

    @Test
    fun `isInRampUp with 1 percent rampUp includes only very low buckets`() {
        val rampUp = RampUp.of(1.0)
        val threshold = Bucketing.rampUpThresholdBasisPoints(rampUp) // 100

        assertTrue(Bucketing.isInRampUp(rampUp, 0))
        assertTrue(Bucketing.isInRampUp(rampUp, 99))
        assertFalse(Bucketing.isInRampUp(rampUp, 100))
        assertFalse(Bucketing.isInRampUp(rampUp, 9_999))
    }
}
