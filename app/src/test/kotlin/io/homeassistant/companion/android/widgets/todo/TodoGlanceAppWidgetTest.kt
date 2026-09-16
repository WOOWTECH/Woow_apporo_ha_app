package io.homeassistant.companion.android.widgets.todo

import android.app.Application
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.assertIsChecked
import androidx.glance.appwidget.testing.unit.assertIsNotChecked
import androidx.glance.appwidget.testing.unit.isIndeterminateCircularProgressIndicator
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasNoClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasTextEqualTo
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.database.widget.WidgetBackgroundType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// 必須指定 application,否則 Robolectric 會實例化 manifest 宣告的
// HomeAssistantApplication。它的 onCreate 會把 process 層級的 FailFast handler 換成
// 會呼叫 exitProcess(1) 的版本(debug variant),並開啟 HAStrictMode ——
// 之後同一個 worker 內的每一個測試都活在「任何 StrictMode violation 就殺 JVM」之下。
// 這個風險專案自己寫在 CrashSavingFailFastHandlerTest 的註解裡,但沒有機制保證。
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TodoGlanceAppWidgetTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `Given LoadingState when ScreenForState then it displays CircularProgressIndicator`() = runGlanceAppWidgetUnitTest {
        setContext(context)

        provideComposable {
            ScreenForState(LoadingTodoState)
        }

        // Verify nothing else exists
        onNode(hasTestTag("Screen"))
            .assertDoesNotExist()
        onNode(hasTestTag("EmptyScreen"))
            .assertDoesNotExist()

        onNode(isIndeterminateCircularProgressIndicator())
            .assertHasNoClickAction()
            .assertExists()
    }

    @Test
    fun `Given EmptyState when ScreenForState then it displays EmptyScreen`() = runGlanceAppWidgetUnitTest {
        setContext(context)

        provideComposable {
            ScreenForState(EmptyTodoState)
        }

        // Verify nothing else exists
        onNode(hasTestTag("LoadingScreen"))
            .assertDoesNotExist()
        onNode(hasTestTag("Screen"))
            .assertDoesNotExist()

        onNode(hasTestTag("EmptyScreen"))
            .assertExists()

        onNode(hasTextEqualTo(context.getString(R.string.widget_no_configuration)))
    }

    @Test
    fun `Given TodoState with empty items when ScreenForState then it displays the empty data`() = runGlanceAppWidgetUnitTest {
        setContext(context)

        val expectedTitle = "Hello world HA"

        provideComposable {
            ScreenForState(
                TodoStateWithData(
                    backgroundType = WidgetBackgroundType.DYNAMICCOLOR,
                    textColor = null,
                    serverId = 1,
                    listEntityId = "",
                    listName = expectedTitle,
                    todoItems = emptyList(),
                    outOfSync = true,
                    showComplete = true,
                ),
            )
        }

        // Verify nothing else exists
        onNode(hasTestTag("EmptyScreen"))
            .assertDoesNotExist()
        onNode(hasTestTag("LoadingScreen"))
            .assertDoesNotExist()

        onNode(hasTestTag("Screen"))
            .assertExists()

        assertTitleBar(expectedTitle)

        onNode(hasTextEqualTo(context.getString(R.string.widget_todo_empty)))
            .assertExists()
    }

    @Test
    fun `Given TodoState with item complete and active and showComplete when ScreenForState then it displays everything`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        val expectedTitle = "Hello world HA"

        provideComposable {
            ScreenForState(
                TodoStateWithData(
                    backgroundType = WidgetBackgroundType.DYNAMICCOLOR,
                    textColor = null,
                    serverId = 1,
                    listEntityId = "",
                    listName = expectedTitle,
                    todoItems = listOf(
                        TodoItemState(
                            uid = "",
                            name = "Hello",
                            done = false,
                        ),
                        TodoItemState(
                            uid = "",
                            name = "World",
                            done = true,
                        ),
                    ),
                    outOfSync = true,
                    showComplete = true,
                ),
            )
        }

        // Verify nothing else exists
        onNode(hasTestTag("EmptyScreen"))
            .assertDoesNotExist()
        onNode(hasTestTag("LoadingScreen"))
            .assertDoesNotExist()

        onNode(hasTestTag("Screen"))
            .assertExists()

        assertTitleBar(expectedTitle)

        onNode(hasTextEqualTo(context.getString(R.string.widget_todo_empty)))
            .assertDoesNotExist()
        onNode(hasTextEqualTo("Hello"))
            .assertExists()
            .assertIsNotChecked()
        onNode(hasTextEqualTo("World"))
            .assertExists()
            .assertIsChecked()
    }

    @Test
    fun `Given TodoState with only active items and showComplete when ScreenForState then it displays only active`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        val expectedTitle = "Hello world HA"

        provideComposable {
            ScreenForState(
                TodoStateWithData(
                    backgroundType = WidgetBackgroundType.DYNAMICCOLOR,
                    textColor = null,
                    serverId = 1,
                    listEntityId = "",
                    listName = expectedTitle,
                    todoItems = listOf(
                        TodoItemState(
                            uid = "",
                            name = "Hello",
                            done = false,
                        ),
                        TodoItemState(
                            uid = "",
                            name = "World",
                            done = false,
                        ),
                    ),
                    outOfSync = true,
                    showComplete = true,
                ),
            )
        }

        // Verify nothing else exists
        onNode(hasTestTag("EmptyScreen"))
            .assertDoesNotExist()
        onNode(hasTestTag("LoadingScreen"))
            .assertDoesNotExist()

        onNode(hasTestTag("Screen"))
            .assertExists()

        assertTitleBar(expectedTitle)

        onNode(hasTextEqualTo(context.getString(R.string.widget_todo_empty)))
            .assertDoesNotExist()
        onNode(hasTextEqualTo("Hello"))
            .assertExists()
            .assertIsNotChecked()
        onNode(hasTextEqualTo("World"))
            .assertExists()
            .assertIsNotChecked()
    }

    @Test
    fun `Given TodoState with item complete and active and not showComplete when ScreenForState then it displays only active`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        val expectedTitle = "Hello world HA"

        provideComposable {
            ScreenForState(
                TodoStateWithData(
                    backgroundType = WidgetBackgroundType.DYNAMICCOLOR,
                    textColor = null,
                    serverId = 1,
                    listEntityId = "",
                    listName = expectedTitle,
                    todoItems = listOf(
                        TodoItemState(
                            uid = "",
                            name = "Hello",
                            done = false,
                        ),
                        TodoItemState(
                            uid = "",
                            name = "World",
                            done = true,
                        ),
                    ),
                    outOfSync = true,
                    showComplete = false,
                ),
            )
        }

        // Verify nothing else exists
        onNode(hasTestTag("EmptyScreen"))
            .assertDoesNotExist()
        onNode(hasTestTag("LoadingScreen"))
            .assertDoesNotExist()

        onNode(hasTestTag("Screen"))
            .assertExists()

        assertTitleBar(expectedTitle)

        onNode(hasTextEqualTo(context.getString(R.string.widget_todo_empty)))
            .assertDoesNotExist()
        onNode(hasTextEqualTo("Hello"))
            .assertExists()
            .assertIsNotChecked()
        onNode(hasTextEqualTo("World"))
            .assertDoesNotExist()
    }

    private fun GlanceAppWidgetUnitTest.assertTitleBar(title: String) {
        onNode(hasTextEqualTo(title))
            .assertExists()

        onNode(hasContentDescriptionEqualTo(context.getString(R.string.widget_todo_refresh)))
            .assertExists()
        onNode(hasTestTag("Refresh"))
            .assertExists()
            .assertHasClickAction()

        onNode(hasContentDescriptionEqualTo(context.getString(R.string.widget_todo_add)))
            .assertExists()
        onNode(hasTestTag("Add"))
            .assertExists()
            .assertHasClickAction()

        // TODO I don't know how to assert the imageProvider or event if it possible
    }
}
