package com.hurry.voicechat

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private const val COUNTRY_FLAG_ASSET_BASE =
    "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

private val countryFlagAssets = mapOf(
    "india" to "IMG_20261108_123655.png",
    "in" to "IMG_20261108_123655.png",
    "ind" to "IMG_20261108_123655.png",
    "singapore" to "IMG_20261108_123737.png",
    "sg" to "IMG_20261108_123737.png",
    "sgp" to "IMG_20261108_123737.png",
    "pakistan" to "IMG_20261108_123805.png",
    "pk" to "IMG_20261108_123805.png",
    "pak" to "IMG_20261108_123805.png",
    "usa" to "IMG_20261108_123833.png",
    "us" to "IMG_20261108_123833.png",
    "united states" to "IMG_20261108_123833.png",
    "united states of america" to "IMG_20261108_123833.png",
    "south africa" to "IMG_20261108_123900.png",
    "za" to "IMG_20261108_123900.png",
    "zaf" to "IMG_20261108_123900.png",
    "turkey" to "IMG_20261108_123932.png",
    "türkiye" to "IMG_20261108_123932.png",
    "tr" to "IMG_20261108_123932.png",
    "tur" to "IMG_20261108_123932.png",
    "uk" to "IMG_20261108_124023.png",
    "gb" to "IMG_20261108_124023.png",
    "gbr" to "IMG_20261108_124023.png",
    "united kingdom" to "IMG_20261108_124023.png",
    "great britain" to "IMG_20261108_124023.png",
    "uae" to "IMG_20261108_124048.png",
    "ae" to "IMG_20261108_124048.png",
    "are" to "IMG_20261108_124048.png",
    "united arab emirates" to "IMG_20261108_124048.png",
    "south korea" to "IMG_20261108_124124.png",
    "korea" to "IMG_20261108_124124.png",
    "republic of korea" to "IMG_20261108_124124.png",
    "korea republic of" to "IMG_20261108_124124.png",
    "kr" to "IMG_20261108_124124.png",
    "kor" to "IMG_20261108_124124.png"
)

private val emojiFlagAssets = mapOf(
    "🇮🇳" to "IMG_20261108_123655.png",
    "🇸🇬" to "IMG_20261108_123737.png",
    "🇵🇰" to "IMG_20261108_123805.png",
    "🇺🇸" to "IMG_20261108_123833.png",
    "🇿🇦" to "IMG_20261108_123900.png",
    "🇹🇷" to "IMG_20261108_123932.png",
    "🇬🇧" to "IMG_20261108_124023.png",
    "🇦🇪" to "IMG_20261108_124048.png",
    "🇰🇷" to "IMG_20261108_124124.png"
)

private fun countryFlagImageUrl(country: String): String? {
    val value = country.trim()
    if (value.isEmpty() || value.equals("null", ignoreCase = true)) return null

    val emojiAsset = emojiFlagAssets.entries.firstOrNull { (emoji, _) -> value.contains(emoji) }?.value
    val normalized = value
        .lowercase()
        .replace(Regex("[^a-z ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val asset = emojiAsset ?: countryFlagAssets[normalized] ?: run {
        // Native records sometimes contain ISO flag emoji generated from their country code.
        val generatedFlag = countryFlag(value)
        emojiFlagAssets[generatedFlag]
    } ?: return null

    return COUNTRY_FLAG_ASSET_BASE + asset
}

@Composable
fun CountryFlagImage(
    country: String,
    modifier: Modifier = Modifier.size(width = 20.dp, height = 14.dp)
) {
    val imageUrl = countryFlagImageUrl(country)
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$country country flag",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
