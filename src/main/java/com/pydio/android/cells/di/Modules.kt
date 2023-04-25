package com.pydio.android.cells.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkerParameters
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.auth.AuthDB
import com.pydio.android.cells.db.preferences.CELLS_PREFERENCES_NAME
import com.pydio.android.cells.db.preferences.legacyMigrations
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AppCredentialService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PasswordStore
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.ScopeService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.services.TokenStore
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.services.TreeNodeRepository
import com.pydio.android.cells.services.WorkerService
import com.pydio.android.cells.services.workers.OfflineSyncWorker
import com.pydio.android.cells.ui.browse.models.AccountHomeVM
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.browse.models.CarouselVM
import com.pydio.android.cells.ui.browse.models.FilterTransferByMenuVM
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.browse.models.SingleTransferVM
import com.pydio.android.cells.ui.browse.models.SortByMenuVM
import com.pydio.android.cells.ui.browse.models.TransfersVM
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.migration.MigrationVM
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.DownloadVM
import com.pydio.android.cells.ui.models.SelectTargetVM
import com.pydio.android.cells.ui.search.SearchVM
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.system.models.JobListVM
import com.pydio.android.cells.ui.system.models.LandingVM
import com.pydio.android.cells.ui.system.models.LogListVM
import com.pydio.android.cells.ui.system.models.PrefReadOnlyVM
import com.pydio.android.cells.ui.system.models.SettingsVM
import com.pydio.cells.api.Server
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.MemoryStore
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Well Known IDs
class DiNames {
    companion object {
        // Dispatchers
        const val uiDispatcher = "uiDispatcher"
        const val ioDispatcher = "ioDispatcher"
        const val cpuDispatcher = "cpuDispatcher"

        // Stores
        const val tokenStore = "TokenStore"
        const val passwordStore = "PasswordStore"
        const val serverStore = "ServerStore"
        const val transportStore = "TransportStore"

    }
}

// Databases are only referenced locally
private const val runtimeDBName = "runtimedb"
private const val authDBName = "authdb"
private const val accountDBName = "accountdb"


val appModule = module {
    single {
        PreferenceDataStoreFactory.create(
            migrations = legacyMigrations(androidContext().applicationContext)
        ) {
            androidContext().applicationContext.preferencesDataStoreFile(CELLS_PREFERENCES_NAME)
        }
    }

    singleOf(::PreferencesService)
}

val dbModule = module {

    // Runtime DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            RuntimeDB::class.java,
            runtimeDBName
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
            authDBName
        )
            .build()
    }

    // Account DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            AccountDB::class.java,
            accountDBName
        ).build()
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

    // Enable better management of context and testing
    single(named(DiNames.uiDispatcher)) {
        Dispatchers.Main
    }
    single(named(DiNames.ioDispatcher)) {
        Dispatchers.IO
    }
    single(named(DiNames.cpuDispatcher)) {
        Dispatchers.Default
    }

    single { ScopeService() }

    single {
        WorkerService(
            androidContext(),
            get(named(DiNames.ioDispatcher)),
            get(named(DiNames.cpuDispatcher)),
            get(),
            get(),
        )
    }

    // Network state
    single {
        NetworkService(
            androidContext(),
            get(named(DiNames.ioDispatcher)),
        )
    }

    // Long running jobs
    single {
        JobService(
            get(named(DiNames.ioDispatcher)),
            get()
        )
    }

    // Authentication
    single<Store<Token>>(named(DiNames.tokenStore)) { TokenStore(get()) }
    single<Store<String>>(named(DiNames.passwordStore)) { PasswordStore(get()) }

    single<Store<Server>>(named(DiNames.serverStore)) { MemoryStore() }
    single<Store<Transport>>(named(DiNames.transportStore)) { MemoryStore() }

    single {
        AppCredentialService(
            get(named(DiNames.tokenStore)),
            get(named(DiNames.passwordStore)),
            get(named(DiNames.transportStore)),
            get(named(DiNames.ioDispatcher)),
            get(named(DiNames.cpuDispatcher)),
            get(), // NetworkService
            get(), // SessionViewDao
            get(), // AccountDao
        )
    }
    single {
        AuthService(
            get(named(DiNames.ioDispatcher)),
            get()
        )
    }
    single {
        FileService(
            get(named(DiNames.ioDispatcher)),
            get()
        )
    }

    // Accounts
    single {
        TreeNodeRepository(
            androidContext().applicationContext,
            get(named(DiNames.ioDispatcher)),
            get()
        )
    }

    // Sessions
    single {
        SessionFactory(
            get(),
            get(),
            get(named(DiNames.serverStore)),
            get(named(DiNames.transportStore)),
            get()
        )
    }
    single {
        AccountService(
            get(named(DiNames.ioDispatcher)),
            get(), get(), get(), get(), get(), get()
        )
    }

    // Business services
    single {
        NodeService(
            androidContext().applicationContext,
            get(named(DiNames.ioDispatcher)),
            get(), get(), get(), get()
        )
    }
    single {
        OfflineService(
            get(named(DiNames.ioDispatcher)),
            get(), get(), get(), get()
        )
    }
    single {
        TransferService(
            get(named(DiNames.ioDispatcher)),
            get(), get(), get(), get(), get(), get()
        )
    }

    single {
        ConnectionService(
            get(named(DiNames.ioDispatcher)),
            get(), get(), get()
        )
    }

    worker { (workerParams: WorkerParameters) ->
        OfflineSyncWorker(
            appContext = get(),
            params = workerParams,
        )
    }
}

val viewModelModule = module {

    viewModelOf(::LandingVM)
    viewModelOf(::MigrationVM)

    viewModel { LoginVM(get(), get(), get()) }
    viewModelOf(::AccountListVM)

    viewModelOf(::NodeActionsVM)

    viewModelOf(::TreeNodeVM)
    viewModelOf(::SortByMenuVM)
    viewModelOf(::FilterTransferByMenuVM)

    viewModelOf(::SettingsVM)
    viewModelOf(::PrefReadOnlyVM)

    viewModelOf(::SearchVM)

    viewModel { HouseKeepingVM(get()) }

    viewModelOf(::DownloadVM)

    viewModelOf(::BookmarksVM)
    viewModelOf(::OfflineVM)
    viewModelOf(::TransfersVM)
    viewModelOf(::MonitorUploadsVM)
    viewModelOf(::SingleTransferVM)

//    viewModel { BrowseHostVM(get()) }
    viewModelOf(::ShareVM)

    viewModelOf(::CarouselVM)

    viewModel { JobListVM(get()) }
    viewModel { LogListVM(get()) }

    viewModelOf(::AccountHomeVM)
    viewModelOf(::BrowseRemoteVM)
    viewModelOf(::FolderVM)

    viewModel { SelectTargetVM(get()) }
}

val allModules = appModule + dbModule + daoModule + serviceModule + viewModelModule
