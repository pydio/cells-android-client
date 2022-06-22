package com.pydio.android.cells

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.pydio.android.cells.databinding.ActivityMainBinding
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.bindings.getWsIconForMenu
import com.pydio.android.cells.ui.home.clearCache
import com.pydio.android.cells.ui.search.SearchFragment
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Central activity for browsing, managing accounts and settings.
 * The various screens are implemented via fragments. See in the ui package.
 */
class MainActivity : AppCompatActivity() {

    private val logTag = MainActivity::class.simpleName

    private val nodeService: NodeService by inject()

    private val prefs: CellsPreferences by inject()
    private val liveSharedPreferences = LiveSharedPreferences(prefs.get())

    private val activeSessionVM: ActiveSessionViewModel by viewModel()

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val encodedState = savedInstanceState?.getString(AppNames.EXTRA_STATE)
            ?: intent.getStringExtra(AppNames.EXTRA_STATE)
        val stateID = StateID.fromId(encodedState)
        Log.d(logTag, "onCreate for: $stateID")
        activeSessionVM.afterCreate(stateID?.accountId)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.mainToolbar)

        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.main_fragment_host)
        appBarConfiguration = AppBarConfiguration(navController.graph, binding.mainDrawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        // Add custom listeners
        binding.navView.setNavigationItemSelectedListener(onMenuItemSelected)

        configureNavigationDrawer()

        handleCustomNavigation(stateID)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(logTag, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.main_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.d(logTag, "onPrepareOptionsMenu: ${StateID.fromId(activeSessionVM.accountId)}")
        super.onPrepareOptionsMenu(menu)
        menu?.let {
            configureSearch(it)
            configureConnexionAlarm(it)
            configureLayoutSwitcher(it)
            configureSort(it)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeSessionVM.accountId?.let {
            Log.i(logTag, "onSaveInstanceState for: ${StateID.fromId(it)}")
            outState.putString(AppNames.EXTRA_STATE, it)
        }
    }

    override fun onResume() {
        Log.e(logTag, "onResume, intent: $intent")
        super.onResume()
    }

    private fun handleCustomNavigation(stateID: StateID?) {

        // Useless: this is the default
//        if (stateID == null) {
//            navController.navigate(MainNavDirections.openAccountList())
//            return
//        }
//        stateID?.let {
//            val action = when {
//                Str.notEmpty(it.workspace) ->
//                    MainNavDirections.openFolder(it.id)
//                else -> MainNavDirections.openAccountHome(it.id)
//            }
//            navController.navigate(action)
//        }

        stateID?.let {
            when {
                Str.notEmpty(it.workspace) -> {
                    val action = MainNavDirections.openFolder(it.id)
                    navController.navigate(action)
                }
            }
        } ?: let { navController.navigate(MainNavDirections.openAccountList()) }
    }

    private fun configureNavigationDrawer() {
        // Configure navigation View header buttons
        val header = binding.navView.getHeaderView(0)
        val switchAccountBtn = header.findViewById<ImageView>(R.id.nav_header_switch_account)
        switchAccountBtn?.setOnClickListener {
            navController.navigate(MainNavDirections.openAccountList())
            closeDrawer()
        }

        if (activeSessionVM.accountId == null) {
            return
        }
        val accId = activeSessionVM.accountId

        // Observe current live session to update the UI
        activeSessionVM.sessionView.observe(this) {
            it?.let { liveSession ->
                // Set current session info in the Navigation view header
                val headerView = binding.navView.getHeaderView(0)
                val primaryText =
                    headerView.findViewById<TextView>(R.id.nav_header_primary_text)
                primaryText.text = liveSession.username
                val secondaryText =
                    headerView.findViewById<TextView>(R.id.nav_header_secondary_text)
                secondaryText.text = liveSession.url

                // Only show offline page when remote is not legacy
                binding.navView.menu.findItem(R.id.offline_root_list_destination)?.isVisible =
                    !liveSession.isLegacy

                // Force refresh of the navigation view
                binding.navView.invalidate()
            }
        }

        // Workspaces
        val wsMenuSection = binding.navView.menu.findItem(R.id.ws_section)
        activeSessionVM.workspaces.observe(this) {
            if (it.isNotEmpty()) {
                wsMenuSection.subMenu.clear()
                for (ws in it) {
                    val wsItem = wsMenuSection.subMenu.add(ws.label)
                    wsItem.icon = ContextCompat.getDrawable(this, getWsIconForMenu(ws))
                    wsItem.setOnMenuItemClickListener {
                        val state = StateID.fromId(accId).withPath("/${ws.slug}")
                        navController.navigate(MainNavDirections.openFolder(state.id))
                        closeDrawer()
                        true
                    }
                }
                binding.navView.invalidate()
            }
        }

        // Optional access to log and job tables
        liveSharedPreferences
            .getBoolean(AppNames.PREF_KEY_SHOW_RUNTIME_LISTS, false)
            .observe(this) {
                binding.navView.menu.findItem(R.id.job_list_destination)?.isVisible = it
                binding.navView.menu.findItem(R.id.log_list_destination)?.isVisible = it
            }
    }

    private val onMenuItemSelected = NavigationView.OnNavigationItemSelectedListener {
        Log.i(logTag, "... Item selected: #${it.itemId}")
        var done = false
        when (it.itemId) {
            R.id.offline_root_list_destination -> {
                activeSessionVM.sessionView.value?.let { _ ->
                    navController.navigate(MainNavDirections.openOfflineRoots())
                    done = true
                }
            }
            R.id.bookmark_list_destination -> {
                activeSessionVM.sessionView.value?.let { _ ->
                    navController.navigate(MainNavDirections.openBookmarks())
                    done = true
                }
            }
            R.id.clear_cache -> {
                activeSessionVM.sessionView.value?.let { session ->
                    clearCache(binding.root.context, session.accountID, nodeService)
                    done = true
                }
            }
            else -> done = NavigationUI.onNavDestinationSelected(it, navController)
        }
        if (done) {
            binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
        }
        done
    }

    private fun closeDrawer() {
        binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onStart() {
        Log.d(logTag, "onStart, intent: $intent")
        super.onStart()
    }

    override fun onStop() {
        Log.d(logTag, "onStop, intent: $intent")
        super.onStop()
    }

    override fun onPause() {
        Log.d(logTag, "onPause, intent: $intent")
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val drawer = binding.mainDrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun configureSearch(menu: Menu) {
        val searchItem = menu.findItem(R.id.search_edit_view)
        if (searchItem != null) {
            searchItem.isVisible = needSearch()
            if (searchItem.isVisible) {
                val searchView = searchItem.actionView as SearchView
                searchView.setOnQueryTextListener(SearchListener())
            }
        }
    }

    private fun configureConnexionAlarm(menu: Menu) {

        // The connection alarm button
        val connexionAlarmBtn = menu.findItem(R.id.open_connexion_dialog)
        connexionAlarmBtn.isVisible = false
        if (activeSessionVM.accountId == null) {
            // no accountID => we are on the account page and the re-log button is not shown
            return
        }
        activeSessionVM.sessionView.observe(this) {
            it?.let { liveSession ->
                val isDisconnected = liveSession.authStatus != AppNames.AUTH_STATUS_CONNECTED
                connexionAlarmBtn.isVisible = isDisconnected
            }
        }
        connexionAlarmBtn.setOnMenuItemClickListener {
            activeSessionVM.sessionView.value?.let {
                val action = MainNavDirections.openManageConnection(it.accountID)
                navController.navigate(action)
            }
            return@setOnMenuItemClickListener true
        }

        // The "no internet banner"
        activeSessionVM.networkInfo.observe(this) {
            // Log.e(logTag, "--- observing network info, new event")
            it?.let { networkInfo ->
                val offlineBannerState = if (networkInfo.isOffline())
                    View.VISIBLE else View.GONE
                /*  Log.e(
                      logTag, "--- current status: ${networkInfo.status} " +
                              "(disconnected: ${networkInfo.isOffline()})"
                  )*/
                binding.offlineBanner.visibility = offlineBannerState
                binding.executePendingBindings()
            }
        }
    }

    private fun configureSort(menu: Menu) {
        val openSortButton = menu.findItem(R.id.open_sort_by)
        openSortButton.isVisible = needListOptions()
        if (!openSortButton.isVisible) {
            return
        }
        openSortButton.setOnMenuItemClickListener {
            val action = MainNavDirections.openSortBy()
            navController.navigate(action)
            return@setOnMenuItemClickListener true
        }
    }

    private fun configureLayoutSwitcher(menu: Menu) {
        val layoutSwitcher = menu.findItem(R.id.switch_recycler_layout)
        val showSwitch = needListOptions()

        layoutSwitcher.isVisible = showSwitch
        if (!showSwitch) {
            return
        }

        liveSharedPreferences
            .getString(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT, AppNames.RECYCLER_LAYOUT_LIST)
            .observe(this) {
                if (it == null) {
                    return@observe
                }

                when (it) {
                    AppNames.RECYCLER_LAYOUT_GRID -> {
                        layoutSwitcher.icon = getIcon(R.drawable.ic_baseline_view_list_24)
                        layoutSwitcher.title = getText(R.string.button_switch_to_list_layout)
                    }
                    else -> {
                        layoutSwitcher.icon = getIcon(R.drawable.ic_sharp_grid_view_24)
                        layoutSwitcher.title = getText(R.string.button_switch_to_grid_layout)
                    }
                }
            }

        layoutSwitcher.setOnMenuItemClickListener {

            val newValue = when (prefs.getString(
                AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT,
                AppNames.RECYCLER_LAYOUT_LIST
            )) {
                AppNames.RECYCLER_LAYOUT_GRID -> AppNames.RECYCLER_LAYOUT_LIST
                else -> AppNames.RECYCLER_LAYOUT_GRID
            }
            prefs.setString(AppNames.PREF_KEY_CURR_RECYCLER_LAYOUT, newValue)
//             Log.e(logTag, "in listener, new layout value: $newValue")
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//            this.recreate()
            return@setOnMenuItemClickListener true
        }
    }

    private fun needSearch(): Boolean {
        return navController.currentDestination?.let {
            when (it.id) {
                R.id.account_home_destination -> true
                R.id.search_destination -> true
                R.id.browse_folder_destination -> true
                else -> false
            }
        } ?: false
    }

    private fun needListOptions(): Boolean {
        return navController.currentDestination?.let {
            when (it.id) {
                R.id.search_destination -> true
                R.id.bookmark_list_destination -> true
                R.id.browse_folder_destination -> true
                R.id.offline_root_list_destination -> true
                else -> false
            }
        } ?: false
    }

    private fun getIcon(id: Int): Drawable? {
        return ResourcesCompat.getDrawable(resources, id, theme)
    }

    private inner class SearchListener : OnQueryTextListener {
        // FIXME clean this class: why local state? Also do externalize.
        private var searchFragment: SearchFragment? = null
        private var stateId: StateID? = null
        private var uiContext: String? = null

        override fun onQueryTextChange(newText: String): Boolean {
            if (Str.empty(newText)) return true
            // TODO for the time being, we do not remote query at each key stroke.
//            navController.currentDestination?.let {
//                if (it.id == R.id.search_destination) {
//                    getSearchFragment()?.updateQuery(newText)
//                }
//            }
            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            navController.currentDestination?.let {
                if (it.id == R.id.search_destination) {
                    getSearchFragment()?.updateQuery(query)
                } else {
                    retrieveCurrentContext()
                    stateId?.let { state ->
                        val action =
                            MainNavDirections.searchEditView(state.id, uiContext!!, query)
                        navController.navigate(action)
                    }
                }
            }
            return true
        }

        private fun retrieveCurrentContext() {
            if (activeSessionVM.sessionView.value == null) {
                showMessage(baseContext, "Cannot search with no active session")
                return
            }
            // showMessage(baseContext, "About to navigate")

            stateId = StateID.fromId(activeSessionVM.sessionView.value!!.accountID)
            uiContext = when (navController.currentDestination!!.id) {
                R.id.bookmark_list_destination -> AppNames.CUSTOM_PATH_BOOKMARKS
                else -> ""
            }
        }

        private fun getSearchFragment(): SearchFragment? {
            searchFragment?.let { return it }
            supportFragmentManager.findFragmentById(R.id.main_fragment_host)
                ?.childFragmentManager?.findFragmentById(R.id.main_fragment_host)?.let {
                    searchFragment = it as SearchFragment
                }
            return searchFragment
        }
    }
}
