package io.github.mrpotatosse.merkator.enumerations

enum class MapTypeEnum(val id: Byte) {
    OUTDOOR(0),
    INDOOR(1);

    companion object {
        private val BY_ID = entries.associateBy(MapTypeEnum::id)
        fun fromId(id: Byte) = BY_ID[id] ?: OUTDOOR
    }
}