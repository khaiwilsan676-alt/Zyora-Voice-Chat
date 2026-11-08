package com.hurry.voicechat

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private const val COUNTRY_FLAG_ASSET_BASE = "https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/"

private val countryFlagAssets = mapOf(
    "india" to "IMG_20261108_123655.png",
    "in" to "IMG_20261108_123655.png",\n    "ind" to "IMG_20261108_123655.png",
    "singapore" to "IMG_20261108_123737.png",
    "sg" to "IMG_20261108_123737.png",\n    "sgp" to "IMG_20261108_123737.png",
    "pakistan" to "IMG_20261108_123805.png",
    "pk" to "IMG_20261108_123805.png",\n    "pak" to "IMG_20261108_123805.png",
    "usa" to "IMG_20261108_123833.png",
    "us" to "IMG_20261108_123833.png",\n    "usa" to "IMG_20261108_123833.png",\n    "united states" to "IMG_20261108_123833.png",
    "united states" to "IMG_20261108_123833.png",
    "united states of america" to "IMG_20261108_123833.png",
    "south africa" to "IMG_20261108_123900.png",
    "za" to "IMG_20261108_123900.png",\n    "zaf" to "IMG_20261108_123900.png",
    "turkey" to "IMG_20261108_123932.png",
    "türkiye" to "IMG_20261108_123932.png",
    "tr" to "IMG_20261108_123932.png",\n    "tur" to "IMG_20261108_123932.png",
    "uk" to "IMG_20261108_124023.png",
    "gb" to "IMG_20261108_124023.png",\n    "gbr" to "IMG_20261108_124023.png",
    "united kingdom" to "IMG_20261108_124023.png",
    "great britain" to "IMG_20261108_124023.png",
    "uae" to "IMG_20261108_124048.png",
    "ae" to "IMG_20261108_124048.png",\n    "are" to "IMG_20261108_124048.png",
    "united arab emirates" to "IMG_20261108_124048.png",
    "south korea" to "IMG_20261108_124124.png",
    "korea" to "IMG_20261108_124124.png",
    "republic of korea" to "IMG_20261108_124124.png",
    "korea republic of" to "IMG_20261108_124124.png",
    "kr" to "IMG_20261108_124124.png",\n    "kor" to "IMG_20261108_124124.png"
)

private fun countryFlagImageUrl(country: String): String? {
    val raw = country.trim().lowercase()
    // Some profile/room records contain only a flag emoji instead of a country name.
    // Resolve the flag before stripping non-ASCII characters.
    val emojiAliases = listOf(
        "🇮🇳" to "india",
        "🇸🇬" to "singapore",
        "🇵🇰" to "pakistan",
        "🇺🇸" to "usa",
        "🇿🇦" to "south africa",
        "🇹🇷" to "turkey",
        "🇬🇧" to "uk",
        "🇦🇪" to "uae",
        "🇰🇷" to "south korea"
    )
    val emojiCountry = emojiAliases.firstOrNull { (emoji, _) -> raw.contains(emoji) }?.second
    val normalized = (emojiCountry ?: raw)
        .replace(Regex("[^a-z ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val file = countryFlagAssets[normalized] ?: return null
    return COUNTRY_FLAG_ASSET_BASE + file
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
