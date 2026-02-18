package com.echotts.app.api.models

import com.google.gson.Gson

data class TtsRequest(
    val behaviorId: String = "PREVIEW",
    val sequenceJson: String,
    val status: String = "ENABLED"
) {
    companion object {
        private val gson = Gson()

        /**
         * Build a TTS request for a single device.
         */
        fun forDevice(
            text: String,
            device: Device,
            locale: String = "en-US"
        ): TtsRequest {
            val operationNode = mapOf(
                "@type" to "com.amazon.alexa.behaviors.model.OpaquePayloadOperationNode",
                "type" to "Alexa.Speak",
                "operationPayload" to mapOf(
                    "deviceType" to device.deviceType,
                    "deviceSerialNumber" to device.serialNumber,
                    "locale" to locale,
                    "customerId" to device.deviceOwnerCustomerId,
                    "textToSpeak" to text
                )
            )
            val sequence = mapOf(
                "@type" to "com.amazon.alexa.behaviors.model.Sequence",
                "startNode" to operationNode
            )
            return TtsRequest(sequenceJson = gson.toJson(sequence))
        }

        /**
         * Build a TTS request that broadcasts to all devices in parallel.
         */
        fun forAllDevices(
            text: String,
            devices: List<Device>,
            locale: String = "en-US"
        ): TtsRequest {
            val nodes = devices.map { device ->
                mapOf(
                    "@type" to "com.amazon.alexa.behaviors.model.OpaquePayloadOperationNode",
                    "type" to "Alexa.Speak",
                    "operationPayload" to mapOf(
                        "deviceType" to device.deviceType,
                        "deviceSerialNumber" to device.serialNumber,
                        "locale" to locale,
                        "customerId" to device.deviceOwnerCustomerId,
                        "textToSpeak" to text
                    )
                )
            }
            val sequence = mapOf(
                "@type" to "com.amazon.alexa.behaviors.model.Sequence",
                "startNode" to mapOf(
                    "@type" to "com.amazon.alexa.behaviors.model.ParallelNode",
                    "nodesToExecute" to nodes
                )
            )
            return TtsRequest(sequenceJson = gson.toJson(sequence))
        }
    }
}
