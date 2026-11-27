package com.northeast.launcher.data

/** Which colors the launcher uses. AUTO follows New England's seasons by the device date. */
enum class ColorSeason(val label: String) {
    AUTO("Automatic (by season)"),
    WINTER("Winter"),
    SPRING("Spring"),
    SUMMER("Summer"),
    FOLIAGE("Foliage"),
    STICK("Stick season");

    /** The season to draw for [month] (1–12). */
    fun resolve(month: Int): Season = when (this) {
        AUTO -> Season.forMonth(month)
        WINTER -> Season.WINTER
        SPRING -> Season.SPRING
        SUMMER -> Season.SUMMER
        FOLIAGE -> Season.FOLIAGE
        STICK -> Season.STICK
    }
}

enum class TreeColor(val label: String) { SEASONAL("Match highlight"), CLASSIC("Classic dark green") }

enum class SceneryMode(val label: String) { FULL("Full pine ridge"), FRONT_ONLY("Front trees only"), NONE("None") }

enum class BackgroundStyle(val label: String) { SPRUCE("Spruce green"), NIGHT("Night forest"), BLACK("Black") }

enum class TileSize(val label: String) { SMALL("Small"), MEDIUM("Medium"), LARGE("Large") }

enum class TileArtwork(val label: String) { ICON_AND_NAME("Icon + name"), BANNER("App banner") }

enum class LabelMode(val label: String) { ALWAYS("Always"), ON_FOCUS("When highlighted"), NEVER("Never") }

enum class FocusZoom(val label: String, val scale: Float) {
    OFF("Off", 1.0f), SUBTLE("Subtle", 1.04f), NORMAL("Normal", 1.08f), BIG("Big", 1.12f)
}

enum class OutlineWidth(val label: String, val dp: Int) { THIN("Thin", 2), NORMAL("Normal", 3), THICK("Thick", 5) }

enum class CornerStyle(val label: String, val dp: Int) { ROUNDED("Rounded", 14), SOFT("Soft", 8), SQUARE("Square", 2) }

enum class RowPosition(val label: String) { UPPER("Upper"), CENTER("Middle"), LOWER("Lower") }

enum class EdgePadding(val label: String, val dp: Int) { NONE("None", 0), SMALL("Small", 24), MEDIUM("Medium", 48), LARGE("Large", 72) }

enum class TextSize(val label: String, val scale: Float) { NORMAL("Normal", 1.0f), LARGE("Large", 1.15f), LARGER("Larger", 1.3f) }

enum class ClockFormat(val label: String) { SYSTEM("Follow TV setting"), H12("12-hour"), H24("24-hour") }

/** Times of day are minutes after midnight. A window may run past midnight. */
enum class NightDimWindow(val label: String, val startMinute: Int, val endMinute: Int) {
    OFF("Off", 0, 0),
    NINE_TO_SIX("9 PM – 6 AM", 21 * 60, 6 * 60),
    TEN_TO_SEVEN("10 PM – 7 AM", 22 * 60, 7 * 60),
    ELEVEN_TO_SEVEN("11 PM – 7 AM", 23 * 60, 7 * 60),
    ALWAYS("Always", 0, 24 * 60),
}

enum class DimStrength(val label: String, val alpha: Float) { LIGHT("Light", 0.25f), MEDIUM("Medium", 0.4f), STRONG("Strong", 0.55f) }

enum class PickerFilter(val label: String) { ALL("All apps"), TV_ONLY("TV apps only") }

enum class PickerSort(val label: String) { NAME("A – Z"), TV_FIRST("TV apps first") }

enum class HomePressAction(val label: String) { FIRST_APP("Jump to first app"), STAY("Stay where you are") }

/**
 * Every user-adjustable option. Defaults reproduce the look chosen for M2.
 * Stored field by field (see [SettingsStore]) so adding a setting never breaks old saves.
 */
data class LauncherSettings(
    // Look & color
    val colorSeason: ColorSeason = ColorSeason.AUTO,
    val treeColor: TreeColor = TreeColor.SEASONAL,
    val scenery: SceneryMode = SceneryMode.FULL,
    val background: BackgroundStyle = BackgroundStyle.SPRUCE,
    val nightDim: NightDimWindow = NightDimWindow.OFF,
    val dimStrength: DimStrength = DimStrength.MEDIUM,
    // Tiles
    val tileSize: TileSize = TileSize.MEDIUM,
    val tileArtwork: TileArtwork = TileArtwork.ICON_AND_NAME,
    val labelMode: LabelMode = LabelMode.ALWAYS,
    val focusZoom: FocusZoom = FocusZoom.NORMAL,
    val outlineWidth: OutlineWidth = OutlineWidth.NORMAL,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val rowPosition: RowPosition = RowPosition.CENTER,
    val showEditTile: Boolean = true,
    // Clock & text
    val showClock: Boolean = true,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = false,
    val showGreeting: Boolean = false,
    val showSeasonName: Boolean = false,
    val showHint: Boolean = true,
    val textSize: TextSize = TextSize.NORMAL,
    val edgePadding: EdgePadding = EdgePadding.NONE,
    // Navigation & apps
    val wrapAround: Boolean = false,
    val rememberFocus: Boolean = true,
    val homePressAction: HomePressAction = HomePressAction.FIRST_APP,
    val pickerFilter: PickerFilter = PickerFilter.ALL,
    val pickerSort: PickerSort = PickerSort.NAME,
    // Home button & remote
    val homeTakeover: Boolean = true,
    /** An extra remote button (Android key code) treated as Home. 0 = none. */
    val extraHomeKey: Int = 0,
    // Lock
    val layoutLocked: Boolean = false,
    /** "salt:hash" from [Pin.create], or empty when no PIN is set. */
    val pinHash: String = "",
) {
    val hasPin: Boolean get() = pinHash.isNotEmpty()
}

/** Saves and loads [LauncherSettings]. Unknown or corrupt values fall back to defaults per field. */
class SettingsStore(private val store: KeyValueStore) {

    fun load(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            colorSeason = enumOr("colorSeason", d.colorSeason),
            treeColor = enumOr("treeColor", d.treeColor),
            scenery = enumOr("scenery", d.scenery),
            background = enumOr("background", d.background),
            nightDim = enumOr("nightDim", d.nightDim),
            dimStrength = enumOr("dimStrength", d.dimStrength),
            tileSize = enumOr("tileSize", d.tileSize),
            tileArtwork = enumOr("tileArtwork", d.tileArtwork),
            labelMode = enumOr("labelMode", d.labelMode),
            focusZoom = enumOr("focusZoom", d.focusZoom),
            outlineWidth = enumOr("outlineWidth", d.outlineWidth),
            cornerStyle = enumOr("cornerStyle", d.cornerStyle),
            rowPosition = enumOr("rowPosition", d.rowPosition),
            showEditTile = boolOr("showEditTile", d.showEditTile),
            showClock = boolOr("showClock", d.showClock),
            clockFormat = enumOr("clockFormat", d.clockFormat),
            showDate = boolOr("showDate", d.showDate),
            showGreeting = boolOr("showGreeting", d.showGreeting),
            showSeasonName = boolOr("showSeasonName", d.showSeasonName),
            showHint = boolOr("showHint", d.showHint),
            textSize = enumOr("textSize", d.textSize),
            edgePadding = enumOr("edgePadding", d.edgePadding),
            wrapAround = boolOr("wrapAround", d.wrapAround),
            rememberFocus = boolOr("rememberFocus", d.rememberFocus),
            homePressAction = enumOr("homePressAction", d.homePressAction),
            pickerFilter = enumOr("pickerFilter", d.pickerFilter),
            pickerSort = enumOr("pickerSort", d.pickerSort),
            homeTakeover = boolOr("homeTakeover", d.homeTakeover),
            extraHomeKey = intOr("extraHomeKey", d.extraHomeKey),
            layoutLocked = boolOr("layoutLocked", d.layoutLocked),
            pinHash = store.getString(key("pinHash"))?.takeIf(Pin::isStoredFormat) ?: d.pinHash,
        )
    }

    fun save(s: LauncherSettings) {
        put("colorSeason", s.colorSeason.name)
        put("treeColor", s.treeColor.name)
        put("scenery", s.scenery.name)
        put("background", s.background.name)
        put("nightDim", s.nightDim.name)
        put("dimStrength", s.dimStrength.name)
        put("tileSize", s.tileSize.name)
        put("tileArtwork", s.tileArtwork.name)
        put("labelMode", s.labelMode.name)
        put("focusZoom", s.focusZoom.name)
        put("outlineWidth", s.outlineWidth.name)
        put("cornerStyle", s.cornerStyle.name)
        put("rowPosition", s.rowPosition.name)
        put("showEditTile", s.showEditTile.toString())
        put("showClock", s.showClock.toString())
        put("clockFormat", s.clockFormat.name)
        put("showDate", s.showDate.toString())
        put("showGreeting", s.showGreeting.toString())
        put("showSeasonName", s.showSeasonName.toString())
        put("showHint", s.showHint.toString())
        put("textSize", s.textSize.name)
        put("edgePadding", s.edgePadding.name)
        put("wrapAround", s.wrapAround.toString())
        put("rememberFocus", s.rememberFocus.toString())
        put("homePressAction", s.homePressAction.name)
        put("pickerFilter", s.pickerFilter.name)
        put("pickerSort", s.pickerSort.name)
        put("homeTakeover", s.homeTakeover.toString())
        put("extraHomeKey", s.extraHomeKey.toString())
        put("layoutLocked", s.layoutLocked.toString())
        put("pinHash", s.pinHash)
    }

    private fun key(name: String) = "setting.$name"

    private fun put(name: String, value: String) = store.putString(key(name), value)

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E {
        val raw = store.getString(key(name)) ?: return default
        return enumValues<E>().firstOrNull { it.name == raw } ?: default
    }

    private fun boolOr(name: String, default: Boolean): Boolean =
        when (store.getString(key(name))) {
            "true" -> true
            "false" -> false
            else -> default
        }

    private fun intOr(name: String, default: Int): Int =
        store.getString(key(name))?.toIntOrNull()?.takeIf { it >= 0 } ?: default
}
