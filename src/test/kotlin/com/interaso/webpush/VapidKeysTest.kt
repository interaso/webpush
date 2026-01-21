package com.interaso.webpush

import org.junit.jupiter.api.Test

class VapidKeysTest {
    @Test
    fun shouldCreateVapidKeysFromUncompressedData() {
        VapidKeys.fromUncompressedBytes(
            "BKFS6Ij219kTr_A_tuLrxASPPVBq4LM2cZO6kO8NRK-PuUkDRFipkJ3Gb5eGSaFOnWhfzzZWRm2WdG72DifpwAg",
            "jsoBoGWlka0WZ01JIVxboxxfFGwkOM6aeaBksStO8do",
        )
    }
}
