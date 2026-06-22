package io.github.mrpotatosse.merkator.enumerations

enum class ElementTypeEnum(val id: Byte) {
    GRAPHICAL(2),
    SOUND(33);

    companion object {
        private val BY_ID = entries.associateBy(ElementTypeEnum::id)

        fun fromId(id: Byte) = BY_ID[id]
    }
}