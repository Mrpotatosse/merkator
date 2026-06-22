package io.github.mrpotatosse.merkator.enumerations

enum class GraphicalElementTypeEnum(val id: Byte) {
    NORMAL(0),
    BOUNDING_BOX(1),
    ANIMATED(2),
    ENTITY(3),
    PARTICLES(4),
    BLENDED(5);

    companion object {
        private val BY_ID = entries.associateBy(GraphicalElementTypeEnum::id)
        fun fromId(id: Byte) = BY_ID[id]
    }
}