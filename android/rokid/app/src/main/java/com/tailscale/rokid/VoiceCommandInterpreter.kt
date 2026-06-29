package com.tailscale.rokid

class VoiceCommandInterpreter {
    fun interpret(input: String): VoiceAction {
        val normalized = input.lowercase()
        return when {
            normalized.contains("sleep") || normalized.contains("standby") -> VoiceAction.Sleep
            normalized.contains("resume") || normalized.contains("wake") -> VoiceAction.Resume
            normalized.contains("status") -> VoiceAction.Status
            normalized.contains("node") -> VoiceAction.Nodes
            normalized.contains("start") || normalized.contains("connect") -> VoiceAction.Start
            else -> VoiceAction.Unknown(input)
        }
    }
}

sealed class VoiceAction(val description: String) {
    data object Start : VoiceAction("start daemon")
    data object Sleep : VoiceAction("enter low power mode")
    data object Resume : VoiceAction("resume mesh activity")
    data object Status : VoiceAction("query network status")
    data object Nodes : VoiceAction("query node summary")
    data class Unknown(val raw: String) : VoiceAction("unknown command: $raw")
}
