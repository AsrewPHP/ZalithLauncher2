package com.movtery.zalithlauncher.game.plugin.renderer

abstract class RendererPlugin(
    val id: String,
    val displayName: String,
    val uniqueIdentifier: String,
    val glName: String,
    val eglName: String,
    val path: String,
    val env: Map<String, String>,
    val dlopen: List<String>
)
