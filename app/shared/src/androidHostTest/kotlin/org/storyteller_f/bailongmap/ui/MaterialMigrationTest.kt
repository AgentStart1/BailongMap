package org.storyteller_f.bailongmap.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.storyteller_f.bailongmap.data.model.Place
import org.storyteller_f.bailongmap.ui.map.rememberMapSheetState
import org.storyteller_f.bailongmap.ui.search.SearchBarUi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaterialMigrationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mapSheetsStartHiddenWithIndependentState() {
        lateinit var states: List<SheetState>
        compose.setContent {
            states = listOf(rememberMapSheetState(), rememberMapSheetState(), rememberMapSheetState())
        }
        compose.runOnIdle {
            states.forEach {
                assertEquals(SheetValue.Hidden, it.currentValue)
                assertEquals(SheetValue.Hidden, it.targetValue)
                assertFalse(it.isVisible)
            }
            assertNotSame(states[0], states[1])
            assertNotSame(states[1], states[2])
        }
    }

    @Test
    fun modalSheetExpandsFullyClosesAndReopens() {
        lateinit var state: SheetState
        compose.setContent {
            MaterialTheme {
                state = rememberMapSheetState()
                var visible by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                Button(onClick = { visible = true }) { Text("Open sheet") }
                if (visible) {
                    ModalBottomSheet(sheetState = state, onDismissRequest = { visible = false }) {
                        Button(onClick = {
                            scope.launch {
                                state.hide()
                                visible = false
                            }
                        }) { Text("Close sheet") }
                        Box(Modifier.height(800.dp))
                    }
                }
            }
        }
        repeat(2) {
            compose.onNodeWithText("Open sheet").performClick()
            compose.runOnIdle { assertEquals(SheetValue.Expanded, state.currentValue) }
            compose.onNodeWithText("Close sheet").performClick()
            compose.runOnIdle { assertEquals(SheetValue.Hidden, state.currentValue) }
        }
    }

    @Test
    fun searchResultClickDeliversTheDisplayedPlace() {
        var selected: Place? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    SearchBarUi("Muse", {}, listOf(place), false, true, { selected = it })
                }
            }
        }
        compose.onNodeWithText(place.displayName).assertExists()
        compose.onNodeWithText(place.name).performClick()
        compose.runOnIdle { assertEquals(place, selected) }
    }

    @Test
    fun searchClearDeliversEmptyQuery() {
        var query: String? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    SearchBarUi("Museum", { query = it }, listOf(place), false, true, {})
                }
            }
        }
        compose.onNodeWithContentDescription("清除").performClick()
        compose.runOnIdle { assertEquals("", query) }
    }

    private val place = Place("test", "Museum", "Museum address", 0.0, 0.0, "museum", "tourism")
}
