package com.example.productivityapp.ui.run

internal object RunMapStyleProvider {
    private const val LIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    private const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

    fun styleUrl(isDarkTheme: Boolean): String = if (isDarkTheme) DARK_STYLE_URL else LIGHT_STYLE_URL
}
