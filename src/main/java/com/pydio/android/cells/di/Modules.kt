package com.pydio.android.cells.di

import androidx.room.Room
import androidx.work.WorkerParameters
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.auth.AuthDB
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AccountServiceImpl
import com.pydio.android.cells.services.AppCredentialService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PasswordStore
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.services.TokenStore
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.services.TreeNodeRepository
import com.pydio.android.cells.services.workers.OfflineSync
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.ConnectionVM
import com.pydio.android.cells.ui.aaLegacy.browsexml.BookmarksViewModel
import com.pydio.android.cells.ui.aaLegacy.browsexml.BrowseFolderViewModel
import com.pydio.android.cells.ui.aaLegacy.browsexml.OfflineRootsViewModel
import com.pydio.android.cells.ui.aaLegacy.transferxml.ChooseTargetViewModel
import com.pydio.android.cells.ui.aaLegacy.transferxml.PickFolderViewModel
import com.pydio.android.cells.ui.aaLegacy.transferxml.PickSessionViewModel
import com.pydio.android.cells.ui.aaLegacy.transferxml.TransferViewModel
import com.pydio.android.cells.ui.aaLegacy.utils.DownloadViewModel
import com.pydio.android.cells.ui.aaLegacy.viewer.CarouselViewModel
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.browse.models.BrowseHostVM
import com.pydio.android.cells.ui.browse.models.CarouselVM
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.browse.models.PreferencesVM
import com.pydio.android.cells.ui.browse.models.TransfersVM
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.android.cells.ui.login.aaLegacy.LoginViewModelNew
import com.pydio.android.cells.ui.login.models.NewLoginVM
import com.pydio.android.cells.ui.login.nav.CellsRouteNavigator
import com.pydio.android.cells.ui.login.nav.RouteNavigator
import com.pydio.android.cells.ui.login.nav.StateViewModel
import com.pydio.android.cells.ui.menus.TransferMenuViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuViewModel
import com.pydio.android.cells.ui.models.AccountHomeVM
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.JobListVM
import com.pydio.android.cells.ui.models.LogListVM
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.android.cells.ui.models.MigrationVM
import com.pydio.android.cells.ui.models.SelectTargetVM
import com.pydio.android.cells.ui.models.UploadsVM
import com.pydio.android.cells.ui.search.SearchViewModel
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.system.models.SettingsVM
import com.pydio.cells.api.Server
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.MemoryStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { CellsPreferences(androidContext().applicationContext) }
}

val dbModule = module {

    // Runtime DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            RuntimeDB::class.java,
            "runtimedb"
        )
            .addMigrations(RuntimeDB.MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    // Auth DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            AuthDB::class.java,
            "authdb"
        )
            .build()
    }

    // Account DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            AccountDB::class.java,
            "accountdb"
        )
            .build()
    }
}

val daoModule = module {

    single { get<RuntimeDB>().jobDao() }
    single { get<RuntimeDB>().logDao() }

    single { get<AuthDB>().tokenDao() }
    single { get<AuthDB>().legacyCredentialsDao() }
    single { get<AuthDB>().authStateDao() }

    single { get<AccountDB>().accountDao() }
    single { get<AccountDB>().sessionViewDao() }
    single { get<AccountDB>().sessionDao() }
    single { get<AccountDB>().workspaceDao() }
}

val serviceModule = module {

    // Network state
    single { NetworkService(androidContext()) }

    // Long running jobs
    single { JobService(get()) }

    // Authentication
    single<Store<Token>>(named("TokenStore")) { TokenStore(get()) }
    single<Store<String>>(named("PasswordStore")) { PasswordStore(get()) }
    single {
        AppCredentialService(
            get(named("TokenStore")),
            get(named("PasswordStore")),
            get()
        )
    }
    single { AuthService(get()) }

    // Accounts
    single<Store<Server>>(named("ServerStore")) { MemoryStore() }
    single<Store<Transport>>(named("TransportStore")) { MemoryStore() }
    single<AccountService> { AccountServiceImpl(get(), get(), get(), get(), get(), get()) }

    // Sessions
    single {
        SessionFactory(
            get(),
            get(),
            get(named("ServerStore")),
            get(named("TransportStore")),
            get()
        )
    }

    // Business services
    single { TreeNodeRepository(androidContext().applicationContext, get()) }
    single { NodeService(androidContext().applicationContext, get(), get(), get(), get(), get()) }
    single { FileService(get()) }
    single { TransferService(get(), get(), get(), get(), get(), get()) }

    worker { (workerParams: WorkerParameters) ->
        OfflineSync(
            appContext = get(),
            params = workerParams,
        )
    }
}

val viewModelModule = module {

    viewModel { NewLoginVM(get(), get(), get()) }
    viewModel { LoginVM(get(), get(), get()) }
    viewModel { AccountListVM(get()) }

    viewModel { ConnectionVM(get(), get()) }
    viewModel { NodeActionsVM(get(), get(), get()) }
    viewModel { parameters -> TreeNodeVM(stateID = parameters.get(), get()) }
    // or Constructor DSL
    // viewModelOf(::TreeNodeVM)
    viewModelOf(::PreferencesVM)

    viewModel { SettingsVM(get()) }
    viewModel { HouseKeepingVM(get()) }

    viewModel { BookmarksVM(get(), get()) }
    viewModel { OfflineVM(get(), get(), get()) }
    viewModel { BrowseHostVM(get()) }
    viewModel { CarouselVM(get(), get()) }
    viewModel { ShareVM(get(), get(), get()) }
    viewModel { MonitorUploadsVM(get(), get()) }

    viewModel { ActiveSessionViewModel(get(), get(), get(), get()) }
    viewModel { params -> BrowseFolderViewModel(get(), get(), get(), params.get()) }
    viewModel { params -> TreeNodeMenuViewModel(params.get(), params.get(), get()) }
    viewModel { params -> DownloadViewModel(params.get(), params.get(), get(), get()) }

    viewModel { BookmarksViewModel(get()) }
    viewModel { OfflineRootsViewModel(get(), get(), get(), get()) }

    viewModel { JobListVM(get()) }
    viewModel { LogListVM(get()) }

    viewModel { AccountHomeVM(get()) }
    viewModel { BrowseRemoteVM(get(), get()) }
    viewModel { FolderVM(get()) }
    viewModel { SelectTargetVM(get()) }
    viewModel { MigrationVM(get(), get(), get(), get()) }
    viewModel { UploadsVM(get(), get()) }
    viewModel { TransfersVM(get(), get()) }

    viewModel { ChooseTargetViewModel(get()) }
    viewModel { PickSessionViewModel(get()) }
    viewModel { PickFolderViewModel(get(), get()) }

    viewModel { params -> TransferViewModel(get(), get(), params.get()) }
    viewModel { params -> TransferMenuViewModel(params.get(), params.get(), get()) }

    viewModel { params -> SearchViewModel(params.get(), get()) }

    viewModel { CarouselViewModel(get(), get()) }

    // Experimental
    single<RouteNavigator> { CellsRouteNavigator() }
    viewModel { StateViewModel(get()) }
    viewModel { LoginViewModelNew(get(), get(), get()) }
}

val allModules = appModule + dbModule + daoModule + serviceModule + viewModelModule
