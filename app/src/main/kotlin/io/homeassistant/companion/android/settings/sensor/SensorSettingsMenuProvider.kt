package io.homeassistant.companion.android.settings.sensor

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.util.AppSupportLinks

/**
 * Menu of the sensor list: the search field, the all/enabled/disabled filter and the help link.
 *
 * The host activity owns the menu, so the menu is recreated whenever the activity rebuilds it while
 * the sensor list is still on screen. The active search query therefore has to be restored from the
 * view model rather than read back from the previous [SearchView] instance.
 */
internal class SensorSettingsMenuProvider(
    private val context: Context,
    private val viewModel: SensorSettingsViewModel,
) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_fragment_sensor, menu)

        val searchViewItem = menu.findItem(R.id.action_search)
        val searchView = searchViewItem.actionView as SearchView
        searchView.apply {
            queryHint = context.getString(commonR.string.search_sensors)
            maxWidth = Integer.MAX_VALUE
        }
        // MenuItem expansion is not SearchView iconification (the item uses showAsAction=ifRoom).
        // Restore before listening so recreating the menu never clears the active filter.
        viewModel.searchQuery?.takeIf { it.isNotEmpty() }?.let { query ->
            searchView.isIconified = false
            searchView.setQuery(query, false)
        }
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchView.clearFocus()
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    viewModel.setSensorsSearchQuery(query)
                    return false
                }
            },
        )
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(viewModel.sensorFilter.menuItemId)?.isChecked = true

        menu.findItem(R.id.get_help)?.let {
            it.isVisible = true
            it.intent = Intent(ACTION_VIEW, AppSupportLinks.SENSORS.toUri())
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        R.id.action_show_sensors_all,
        R.id.action_show_sensors_enabled,
        R.id.action_show_sensors_disabled,
        -> {
            menuItem.isChecked = !menuItem.isChecked
            viewModel.setSensorFilterChoice(menuItem.itemId)
            true
        }

        else -> false
    }
}
