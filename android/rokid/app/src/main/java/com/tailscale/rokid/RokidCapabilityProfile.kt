package com.tailscale.rokid

data class RokidCapabilityProfile(
    val productLine: String,
    val supportsSpatialHud: Boolean,
    val supportsGestureInput: Boolean,
    val supportsVoiceInput: Boolean,
    val recommendedHeartbeatSeconds: Int,
) {
    fun describe(): String {
        return "Device profile: $productLine, HUD=$supportsSpatialHud, gesture=$supportsGestureInput, voice=$supportsVoiceInput, heartbeat=${recommendedHeartbeatSeconds}s"
    }

    companion object {
        fun fromDevice(model: String, device: String): RokidCapabilityProfile {
            val normalized = (model + " " + device).lowercase()
            return when {
                normalized.contains("station") -> RokidCapabilityProfile(
                    productLine = "Rokid Station / Master",
                    supportsSpatialHud = true,
                    supportsGestureInput = true,
                    supportsVoiceInput = true,
                    recommendedHeartbeatSeconds = 15,
                )
                normalized.contains("max") || normalized.contains("air") -> RokidCapabilityProfile(
                    productLine = "Rokid Max / Air",
                    supportsSpatialHud = true,
                    supportsGestureInput = false,
                    supportsVoiceInput = true,
                    recommendedHeartbeatSeconds = 20,
                )
                else -> RokidCapabilityProfile(
                    productLine = "Rokid Sprite Compatible",
                    supportsSpatialHud = true,
                    supportsGestureInput = false,
                    supportsVoiceInput = true,
                    recommendedHeartbeatSeconds = 20,
                )
            }
        }
    }
}
