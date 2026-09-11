package com.kelvinsaputra.promptstudio.domain

/** Small, style-independent starting points using the shared costume vocabulary. */
data class OutfitPreset(val id: String, val label: String, val costume: CostumeConfiguration) {
    fun applyTo(project: CharacterProject) = project.copy(costume = costume.copy(customNotes = project.costume.customNotes))
}
object OutfitPresets {
    private val blank = CharacterLibrary.newCharacter("outfit-template").costume
    val entries = listOf(
        OutfitPreset("everyday", "Everyday", blank.copy(outfitIdentity = "practical everyday separates", silhouette = Silhouette.FITTED,
            innerwear = Innerwear.SHIRT, lowerWear = LowerWear.SLIM_TROUSERS, footwear = Footwear.SNEAKERS)),
        OutfitPreset("field", "Field layers", blank.copy(outfitIdentity = "layered field clothing", silhouette = Silhouette.LAYERED,
            outerwear = Outerwear.FIELD_JACKET, innerwear = Innerwear.KNIT, lowerWear = LowerWear.TACTICAL_PANTS, footwear = Footwear.WORK_BOOTS)),
        OutfitPreset("tailored", "Tailored", blank.copy(outfitIdentity = "relaxed tailored separates", silhouette = Silhouette.LONG_LINE,
            outerwear = Outerwear.BLAZER, innerwear = Innerwear.MOCK_NECK, lowerWear = LowerWear.WIDE_TROUSERS, footwear = Footwear.LOAFERS)),
    )
}
