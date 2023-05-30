package ani.saikou.settings

import java.io.Serializable

data class CurrentReaderSettings(
    var direction: Directions = Directions.TOP_TO_BOTTOM,
    var layout: Layouts = Layouts.CONTINUOUS,
    var dualPageMode: DualPageModes = DualPageModes.Automatic,
    var overScrollMode: Boolean = true,
    var trueColors: Boolean = false,
    var rotation: Boolean = true,
    var padding: Boolean = true,
    var hidePageNumbers: Boolean = false,
    var horizontalScrollBar: Boolean = true,
    var keepScreenOn: Boolean = false,
    var volumeButtons: Boolean = false,
    var wrapImages: Boolean = false,
    var longClickImage: Boolean = true,
    var cropBorders: Boolean = false,
    var cropBorderThreshold: Int = 10,
) : Serializable {

    enum class Directions(val string: String) {
        TOP_TO_BOTTOM("Top to Bottom"),
        RIGHT_TO_LEFT("Right to Left"),
        BOTTOM_TO_TOP("Bottom to Top"),
        LEFT_TO_RIGHT("Left to Right");

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class Layouts(val string: String) {
        PAGED("Paged"),
        CONTINUOUS_PAGED("Continuous Paged"),
        CONTINUOUS("Continuous");

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    enum class DualPageModes {
        No, Automatic, Force;

        companion object {
            operator fun get(value: Int) = values().firstOrNull { it.ordinal == value }
        }
    }

    companion object {
        fun applyWebtoon(settings: CurrentReaderSettings) {
            settings.apply {
                layout = Layouts.CONTINUOUS
                direction = Directions.TOP_TO_BOTTOM
                dualPageMode = DualPageModes.No
                padding = false
            }
        }
    }
}

