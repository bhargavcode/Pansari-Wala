package org.bhargav.pansariwala.platform

interface PlateOcr {
    suspend fun readRegistration(imageBytes: ByteArray): Result<String>
}

class FormatPlateOcr : PlateOcr {
    override suspend fun readRegistration(imageBytes: ByteArray): Result<String> {
        if (imageBytes.size < 64) {
            return Result.failure(IllegalArgumentException("Vehicle photo is required"))
        }
        return Result.success("")
    }
}

fun normalizeVehicleReg(value: String): String =
    value.uppercase().replace(" ", "").replace("-", "")
