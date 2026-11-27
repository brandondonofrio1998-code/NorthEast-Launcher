package com.northeast.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun emptyStoreGivesDefaults() {
        assertEquals(LauncherSettings(), SettingsStore(InMemoryKeyValueStore()).load())
    }

    @Test
    fun everyFieldRoundTrips() {
        val custom = LauncherSettings(
            colorSeason = ColorSeason.SUMMER,
            treeColor = TreeColor.CLASSIC,
            scenery = SceneryMode.FRONT_ONLY,
            background = BackgroundStyle.BLACK,
            nightDim = NightDimWindow.TEN_TO_SEVEN,
            dimStrength = DimStrength.STRONG,
            tileSize = TileSize.LARGE,
            tileArtwork = TileArtwork.BANNER,
            labelMode = LabelMode.ON_FOCUS,
            focusZoom = FocusZoom.OFF,
            outlineWidth = OutlineWidth.THICK,
            cornerStyle = CornerStyle.SQUARE,
            rowPosition = RowPosition.LOWER,
            showEditTile = false,
            showClock = false,
            clockFormat = ClockFormat.H24,
            showDate = true,
            showGreeting = true,
            showSeasonName = true,
            showHint = false,
            textSize = TextSize.LARGER,
            edgePadding = EdgePadding.MEDIUM,
            wrapAround = true,
            rememberFocus = false,
            homePressAction = HomePressAction.STAY,
            pickerFilter = PickerFilter.TV_ONLY,
            pickerSort = PickerSort.TV_FIRST,
            homeTakeover = false,
            extraHomeKey = 284,
            layoutLocked = true,
            pinHash = Pin.create("2468"),
        )
        val backing = InMemoryKeyValueStore()
        SettingsStore(backing).save(custom)
        assertEquals(custom, SettingsStore(backing).load())
    }

    @Test
    fun corruptValuesFallBackPerField() {
        val backing = InMemoryKeyValueStore()
        backing.putString("setting.tileSize", "ENORMOUS")
        backing.putString("setting.showClock", "maybe")
        backing.putString("setting.extraHomeKey", "-3")
        backing.putString("setting.pinHash", "1234")
        backing.putString("setting.background", "NIGHT")
        val loaded = SettingsStore(backing).load()
        val defaults = LauncherSettings()
        assertEquals(defaults.tileSize, loaded.tileSize)
        assertEquals(defaults.showClock, loaded.showClock)
        assertEquals(0, loaded.extraHomeKey)
        assertFalse("a plain PIN must never be accepted as a stored hash", loaded.hasPin)
        assertEquals(BackgroundStyle.NIGHT, loaded.background)
    }

    @Test
    fun settingsDoNotCollideWithPinnedApps() {
        val backing = InMemoryKeyValueStore()
        val pins = PinnedAppsRepository(backing, defaultPackages = emptyList())
        pins.save(listOf(AppKey("a.b", "a.b.C")))
        SettingsStore(backing).save(LauncherSettings(tileSize = TileSize.SMALL))
        assertEquals(listOf(AppKey("a.b", "a.b.C")), pins.load())
        assertTrue(SettingsStore(backing).load().tileSize == TileSize.SMALL)
    }

    @Test
    fun colorSeasonResolves() {
        assertEquals(Season.STICK, ColorSeason.AUTO.resolve(11))
        assertEquals(Season.WINTER, ColorSeason.AUTO.resolve(1))
        assertEquals(Season.SUMMER, ColorSeason.SUMMER.resolve(11))
    }
}
