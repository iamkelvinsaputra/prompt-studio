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

/** Distinct template libraries. Garment choices remain freely editable in Build mode. */
data class OutfitTemplate(val category: String, val gender: GenderPresentation, val costume: CostumeConfiguration) {
    val label get() = listOfNotNull(costume.outerwear?.wording, costume.innerwear?.wording, costume.lowerWear?.wording).joinToString(" + ")
    fun applyTo(p: CharacterProject) = p.copy(costume = costume.copy(customNotes = p.costume.customNotes), accessories = p.accessories)
}
object OutfitLibrary {
    val categories = listOf("Casual", "Smart casual", "Streetwear", "Minimal", "Professional", "Elegant", "Sporty", "School-inspired", "Outdoor", "Utility", "Fantasy", "Sci-fi", "Traditional-inspired", "Summer", "Winter", "Evening", "Adventure")
    private fun template(category: String, gender: GenderPresentation, outer: Outerwear?, inner: Innerwear, lower: LowerWear, shoes: Footwear, material: String) = OutfitTemplate(category, gender,
        CharacterLibrary.newCharacter("outfit").costume.copy(outfitIdentity = "${category.lowercase()} separates", outerwear = outer, innerwear = inner, lowerWear = lower, footwear = shoes, materialFeel = material, silhouette = Silhouette.LAYERED))
    val entries = buildList {
        add(template("Casual", GenderPresentation.FEMALE, Outerwear.CROPPED_JACKET, Innerwear.FITTED_TOP, LowerWear.SLIM_TROUSERS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Smart casual", GenderPresentation.FEMALE, Outerwear.CARDIGAN, Innerwear.BLOUSE, LowerWear.SKIRT, Footwear.LOAFERS, "cotton and linen"))
        add(template("Streetwear", GenderPresentation.FEMALE, Outerwear.BOMBER, Innerwear.TANK, LowerWear.CARGOS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Minimal", GenderPresentation.FEMALE, null, Innerwear.MOCK_NECK, LowerWear.WIDE_TROUSERS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Professional", GenderPresentation.FEMALE, Outerwear.BLAZER, Innerwear.FITTED_TOP, LowerWear.SLIM_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Elegant", GenderPresentation.FEMALE, Outerwear.COAT, Innerwear.BLOUSE, LowerWear.ASYMMETRICAL_SKIRT, Footwear.LOAFERS, "cotton and linen"))
        add(template("Sporty", GenderPresentation.FEMALE, Outerwear.ATHLETIC_JACKET, Innerwear.FITTED_TOP, LowerWear.TRAINING_WEAR, Footwear.SNEAKERS, "matte technical fabric"))
        add(template("School-inspired", GenderPresentation.FEMALE, Outerwear.CARDIGAN, Innerwear.SHIRT, LowerWear.SKIRT, Footwear.LOAFERS, "cotton and linen"))
        add(template("Outdoor", GenderPresentation.FEMALE, Outerwear.HOODED_SHELL, Innerwear.KNIT, LowerWear.SLIM_TROUSERS, Footwear.WORK_BOOTS, "matte technical fabric"))
        add(template("Utility", GenderPresentation.FEMALE, Outerwear.CROPPED_JACKET, Innerwear.TACTICAL_UNDERSHIRT, LowerWear.CARGOS, Footwear.WORK_BOOTS, "matte technical fabric"))
        add(template("Fantasy", GenderPresentation.FEMALE, Outerwear.PONCHO, Innerwear.BANDED_WRAP_TOP, LowerWear.ASYMMETRICAL_SKIRT, Footwear.WORK_BOOTS, "cotton and linen"))
        add(template("Sci-fi", GenderPresentation.FEMALE, Outerwear.SLEEVELESS_VEST, Innerwear.MOCK_NECK, LowerWear.PLEATED_HYBRID_BOTTOM, Footwear.COMBAT_BOOTS, "matte technical fabric"))
        add(template("Traditional-inspired", GenderPresentation.FEMALE, Outerwear.COAT, Innerwear.BANDED_WRAP_TOP, LowerWear.WIDE_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Summer", GenderPresentation.FEMALE, null, Innerwear.BLOUSE, LowerWear.SKIRT, Footwear.SANDALS, "cotton and linen"))
        add(template("Winter", GenderPresentation.FEMALE, Outerwear.COAT, Innerwear.KNIT, LowerWear.SLIM_TROUSERS, Footwear.COMBAT_BOOTS, "wool and knitwear"))
        add(template("Evening", GenderPresentation.FEMALE, Outerwear.BLAZER, Innerwear.BLOUSE, LowerWear.WIDE_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Adventure", GenderPresentation.FEMALE, Outerwear.FIELD_JACKET, Innerwear.FITTED_TOP, LowerWear.TACTICAL_PANTS, Footwear.WORK_BOOTS, "matte technical fabric"))
        add(template("Casual", GenderPresentation.MALE, Outerwear.OVERSHIRT, Innerwear.TEE, LowerWear.STRAIGHT_TROUSERS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Smart casual", GenderPresentation.MALE, Outerwear.BLAZER, Innerwear.KNIT, LowerWear.STRAIGHT_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Streetwear", GenderPresentation.MALE, Outerwear.BOMBER, Innerwear.TEE, LowerWear.CARGOS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Minimal", GenderPresentation.MALE, null, Innerwear.SHIRT, LowerWear.STRAIGHT_TROUSERS, Footwear.SNEAKERS, "cotton and linen"))
        add(template("Professional", GenderPresentation.MALE, Outerwear.BLAZER, Innerwear.MOCK_NECK, LowerWear.SLIM_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Elegant", GenderPresentation.MALE, Outerwear.COAT, Innerwear.MOCK_NECK, LowerWear.WIDE_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Sporty", GenderPresentation.MALE, Outerwear.ATHLETIC_JACKET, Innerwear.TEE, LowerWear.TRAINING_WEAR, Footwear.SNEAKERS, "matte technical fabric"))
        add(template("School-inspired", GenderPresentation.MALE, Outerwear.BLAZER, Innerwear.SHIRT, LowerWear.STRAIGHT_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Outdoor", GenderPresentation.MALE, Outerwear.FIELD_JACKET, Innerwear.KNIT, LowerWear.CARGOS, Footwear.WORK_BOOTS, "matte technical fabric"))
        add(template("Utility", GenderPresentation.MALE, Outerwear.SLEEVELESS_VEST, Innerwear.TACTICAL_UNDERSHIRT, LowerWear.TACTICAL_PANTS, Footwear.WORK_BOOTS, "matte technical fabric"))
        add(template("Fantasy", GenderPresentation.MALE, Outerwear.PONCHO, Innerwear.SHIRT, LowerWear.WIDE_TROUSERS, Footwear.WORK_BOOTS, "cotton and linen"))
        add(template("Sci-fi", GenderPresentation.MALE, Outerwear.HOODED_SHELL, Innerwear.TACTICAL_UNDERSHIRT, LowerWear.SLIM_TROUSERS, Footwear.COMBAT_BOOTS, "matte technical fabric"))
        add(template("Traditional-inspired", GenderPresentation.MALE, Outerwear.OVERSHIRT, Innerwear.MOCK_NECK, LowerWear.WIDE_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Summer", GenderPresentation.MALE, null, Innerwear.TEE, LowerWear.SHORTS, Footwear.SANDALS, "cotton and linen"))
        add(template("Winter", GenderPresentation.MALE, Outerwear.COAT, Innerwear.KNIT, LowerWear.STRAIGHT_TROUSERS, Footwear.COMBAT_BOOTS, "wool and knitwear"))
        add(template("Evening", GenderPresentation.MALE, Outerwear.BLAZER, Innerwear.SHIRT, LowerWear.SLIM_TROUSERS, Footwear.LOAFERS, "cotton and linen"))
        add(template("Adventure", GenderPresentation.MALE, Outerwear.HOODED_SHELL, Innerwear.TEE, LowerWear.TACTICAL_PANTS, Footwear.WORK_BOOTS, "matte technical fabric"))
    }
}
