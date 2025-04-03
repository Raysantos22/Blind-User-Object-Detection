package com.BlindDetection.feature.home

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val cnf: Float,
    val cls: Int,
    val clsName: String
) {
    //    fun calculateDistance(): Float {
//        val REFERENCE_HEIGHT = 1.7f // Average human height in meters
//        val REFERENCE_WIDTH = 0.5f  // Average human width in meters
//
//        val distanceByHeight = (REFERENCE_HEIGHT / h) * 0.8f
//        val distanceByWidth = (REFERENCE_WIDTH / w) * 0.8f
//
//        return (distanceByHeight + distanceByWidth) / 2.0f
//    }
    fun calculateDistance(): Float {
        // Define reference dimensions (height, width in meters) for all supported objects
        val referenceDimensions = mapOf(
            // People and animals
            "person" to Pair(1.7f, 0.5f),
            "bird" to Pair(0.2f, 0.3f),
            "cat" to Pair(0.3f, 0.4f),
            "dog" to Pair(0.6f, 0.4f),
            "horse" to Pair(1.6f, 2.0f),
            "sheep" to Pair(0.9f, 1.2f),
            "cow" to Pair(1.4f, 2.0f),
            "elephant" to Pair(3.0f, 5.0f),
            "bear" to Pair(1.5f, 1.8f),
            "zebra" to Pair(1.5f, 2.2f),
            "giraffe" to Pair(4.8f, 2.0f),

            // Vehicles
            "bicycle" to Pair(1.0f, 1.7f),
            "car" to Pair(1.5f, 1.8f),
            "motorcycle" to Pair(1.2f, 2.0f),
            "airplane" to Pair(4.0f, 35.0f),
            "bus" to Pair(3.0f, 12.0f),
            "train" to Pair(3.8f, 20.0f),
            "truck" to Pair(3.5f, 7.0f),
            "boat" to Pair(1.5f, 4.0f),

            // Street objects
            "traffic light" to Pair(0.9f, 0.3f),
            "fire hydrant" to Pair(0.9f, 0.3f),
            "stop sign" to Pair(0.8f, 0.8f),
            "parking meter" to Pair(1.5f, 0.3f),
            "bench" to Pair(0.5f, 1.5f),

            // Accessories
            "backpack" to Pair(0.5f, 0.3f),
            "umbrella" to Pair(1.0f, 0.9f),
            "handbag" to Pair(0.3f, 0.4f),
            "tie" to Pair(0.4f, 0.1f),
            "suitcase" to Pair(0.6f, 0.4f),

            // Sports equipment
            "frisbee" to Pair(0.03f, 0.25f),
            "skis" to Pair(0.1f, 1.8f),
            "snowboard" to Pair(0.3f, 1.5f),
            "sports ball" to Pair(0.2f, 0.2f),
            "kite" to Pair(0.8f, 0.6f),
            "baseball bat" to Pair(0.8f, 0.06f),
            "baseball glove" to Pair(0.3f, 0.2f),
            "skateboard" to Pair(0.15f, 0.8f),
            "surfboard" to Pair(0.25f, 2.0f),
            "tennis racket" to Pair(0.7f, 0.25f),

            // Kitchen items
            "bottle" to Pair(0.25f, 0.08f),
            "wine glass" to Pair(0.15f, 0.07f),
            "cup" to Pair(0.1f, 0.08f),
            "fork" to Pair(0.18f, 0.02f),
            "knife" to Pair(0.2f, 0.02f),
            "spoon" to Pair(0.15f, 0.04f),
            "bowl" to Pair(0.08f, 0.15f),

            // Food
            "banana" to Pair(0.05f, 0.2f),
            "apple" to Pair(0.08f, 0.08f),
            "sandwich" to Pair(0.07f, 0.12f),
            "orange" to Pair(0.08f, 0.08f),
            "broccoli" to Pair(0.15f, 0.12f),
            "carrot" to Pair(0.02f, 0.15f),
            "hot dog" to Pair(0.04f, 0.15f),
            "pizza" to Pair(0.04f, 0.3f),
            "donut" to Pair(0.04f, 0.09f),
            "cake" to Pair(0.08f, 0.25f),

            // Furniture
            "chair" to Pair(0.8f, 0.5f),
            "couch" to Pair(0.9f, 2.0f),
            "potted plant" to Pair(0.8f, 0.4f),
            "bed" to Pair(0.6f, 2.0f),
            "dining table" to Pair(0.8f, 1.5f),
            "toilet" to Pair(0.8f, 0.6f),

            // Electronics
            "tv" to Pair(0.6f, 1.2f),
            "laptop" to Pair(0.25f, 0.35f),
            "mouse" to Pair(0.04f, 0.07f),
            "remote" to Pair(0.02f, 0.15f),
            "keyboard" to Pair(0.03f, 0.4f),
            "cell phone" to Pair(0.14f, 0.07f),
            "microwave" to Pair(0.4f, 0.55f),
            "oven" to Pair(0.6f, 0.6f),
            "toaster" to Pair(0.2f, 0.3f),
            "sink" to Pair(0.35f, 0.6f),
            "refrigerator" to Pair(1.8f, 0.75f),

            // Miscellaneous
            "book" to Pair(0.03f, 0.2f),
            "clock" to Pair(0.3f, 0.3f),
            "vase" to Pair(0.3f, 0.15f),
            "scissors" to Pair(0.02f, 0.15f),
            "teddy bear" to Pair(0.3f, 0.2f),
            "hair drier" to Pair(0.2f, 0.1f),
            "toothbrush" to Pair(0.02f, 0.18f)
        )

        // Get reference dimensions for this object class or use default if not found
        val (refHeight, refWidth) = referenceDimensions[clsName.lowercase()] ?: Pair(1.0f, 0.5f)

        // Calculate distance using both height and width
        // The 0.8f is a calibration factor that can be adjusted based on camera specs
        val distanceByHeight = (refHeight / h) * 0.8f
        val distanceByWidth = (refWidth / w) * 0.8f

        // Categorize objects to apply appropriate weighting
        val isVertical = h > 1.5f * w  // Tall objects like people, stop signs
        val isHorizontal = w > 1.5f * h  // Wide objects like couches, dining tables
        val isSmall = (h < 0.1f || w < 0.1f)  // Small objects need special handling

        // Apply different weights based on object orientation and confidence
        val heightWeight = when {
            isVertical -> 0.7f      // For tall objects, trust height more
            isHorizontal -> 0.3f    // For wide objects, trust width more
            isSmall -> 0.5f         // For small objects, equal weights are safer
            else -> 0.5f            // Default to equal weighting
        }

        // For small objects, distance may be less reliable, apply a correction factor
        val smallObjectCorrection = if (isSmall) 0.9f else 1.0f

        // Confidence-based adjustment - higher confidence means more accurate measurements
        val confidenceFactor = 0.9f + (cnf * 0.1f)

        // Position-based correction - objects at frame edges may appear distorted
        val centerX = 0.5f
        val centerY = 0.5f
        val distanceFromCenterX = Math.abs(cx - centerX)
        val distanceFromCenterY = Math.abs(cy - centerY)
        val distanceFromCenter = Math.sqrt(
            ((distanceFromCenterX * distanceFromCenterX) +
                    (distanceFromCenterY * distanceFromCenterY)).toDouble()
        ).toFloat()

        // Edge correction factor - objects at edges may appear differently
        val edgeCorrection = 1.0f + (distanceFromCenter * 0.15f)

        // Calculate final distance with all corrections
        val baseDistance =
            (distanceByHeight * heightWeight) + (distanceByWidth * (1.0f - heightWeight))
        return baseDistance * smallObjectCorrection * confidenceFactor * edgeCorrection
    }

    /**
     * Enhanced position description for better blind user navigation
     * Provides more detailed directional information
     */
    fun getPositionDescription(): String {
        // More granular position descriptions for better blind user guidance
        return when {
            cx < 0.2f -> "far to your left"
            cx < 0.4f -> "on your left"
            cx >= 0.4f && cx < 0.45f -> "slightly to your left"
            cx >= 0.45f && cx <= 0.55f -> "directly in front of you"
            cx > 0.55f && cx <= 0.6f -> "slightly to your right"
            cx > 0.6f && cx <= 0.8f -> "on your right"
            cx > 0.8f -> "far to your right"
            else -> "in front of you"
        }
    }

    /**
     * Get proximity warning based on object type and distance
     * Provides contextual warnings for navigating blind users
     */
    fun getProximityWarning(distance: Float): String? {
        // Create specific warnings based on object type and distance
        val isObstacle = listOf(
            "chair", "couch", "table", "bed", "bench", "potted plant",
            "person", "dog", "cat", "suitcase", "fire hydrant"
        ).any { clsName.lowercase().contains(it) }

        val isDangerous = listOf(
            "knife", "scissors", "hot", "oven", "stove", "stairs", "glass"
        ).any { clsName.lowercase().contains(it) }

        return when {
            isDangerous && distance < 1.0f ->
                "CAUTION! Potentially dangerous object very close!"

            isObstacle && distance < 0.7f ->
                "WARNING! Obstacle very close, may need to change direction!"

            isObstacle && distance < 1.5f ->
                "Caution, obstacle approaching"

            distance < 0.5f ->
                "Very close object, proceed carefully"

            distance < 1.0f ->
                "Close object, be aware"

            else -> null // No warning needed
        }
    }
}




















//    fun getPositionDescription(): String {
//        return when {
//            cx < 0.4f -> "on your left"
//            cx > 0.6f -> "on your right"
//            else -> "in front of you"
//        }
//    }
//}