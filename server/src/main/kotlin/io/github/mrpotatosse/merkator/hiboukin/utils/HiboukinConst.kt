package io.github.mrpotatosse.merkator.hiboukin.utils

const val DecryptionKey = "649ae451ca33ec53bbcbcc33becf15f4"
val DecryptionKeyBytes = DecryptionKey.toByteArray(Charsets.UTF_8)

const val AppHiboukinPathKey = "app_hiboukin_path"
const val AppHiboukinPathDefault = "default"

const val CellWidth = 86
const val CellHalfWidth = 43
const val CellHeight = 43
const val CellHalfHeight = 21.5

const val MapWidth: Int = 14
const val MapHeight: Int = 20

const val MapRatio = 16.0 / 9.0
const val CanvasHeight = 1024
const val CanvasWidth = (CanvasHeight * MapRatio).toInt()


