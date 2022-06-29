package com.pydio.android.cells.services

import androidx.room.Room
import androidx.work.WorkerParameters
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.auth.AuthDB
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.account.AccountListViewModel
import com.pydio.android.cells.ui.auth.OAuthViewModel
import com.pydio.android.cells.ui.auth.P8CredViewModel
import com.pydio.android.cells.ui.auth.ServerUrlViewModel
import com.pydio.android.cells.ui.browse.BookmarksViewModel
import com.pydio.android.cells.ui.browse.BrowseFolderViewModel
import com.pydio.android.cells.ui.browse.OfflineRootsViewModel
import com.pydio.android.cells.ui.home.JobListViewModel
import com.pydio.android.cells.ui.home.LogListViewModel
import com.pydio.android.cells.ui.menus.TransferMenuViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuViewModel
import com.pydio.android.cells.ui.search.SearchViewModel
import com.pydio.android.cells.ui.transfer.ChooseTargetViewModel
import com.pydio.android.cells.ui.transfer.PickFolderViewModel
import com.pydio.android.cells.ui.transfer.PickSessionViewModel
import com.pydio.android.cells.ui.transfer.TransferViewModel
import com.pydio.android.cells.ui.utils.DownloadViewModel
import com.pydio.android.cells.ui.viewer.CarouselViewModel
import com.pydio.cells.api.Server
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.auth.CredentialService
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.MemoryStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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
        ).build()
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

    single { get<RuntimeDB>().networkInfoDao() }
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
    single { CredentialService(get(named("TokenStore")), get(named("PasswordStore"))) }
    single { AuthService(get()) }

    // Accounts and Sessions
    single<Store<Server>>(named("ServerStore")) { MemoryStore() }
    single<Store<Transport>>(named("TransportStore")) { MemoryStore() }
    single<AccountService> { AccountServiceImpl(get(), get(), get(), get()) }
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
        OfflineSyncWorker(
            accountService = get(),
            nodeService = get(),
            appContext = get(),
            params = workerParams,
        )
    }
}

val viewModelModule = module {

    viewModel { ServerUrlViewModel(get(), get()) }
    viewModel { OAuthViewModel(get(), get(), get()) }
    viewModel { P8CredViewModel(get()) }
    viewModel { AccountListViewModel(get()) }

    viewModel { ActiveSessionViewModel(get(), get(), get(), get()) }
    viewModel { params -> BrowseFolderViewModel(get(), get(), get(), params.get()) }
    viewModel { params -> TreeNodeMenuViewModel(params.get(), params.get(), get()) }
    viewModel { params -> DownloadViewModel(params.get(), params.get(), get(), get()) }

    viewModel { BookmarksViewModel(get()) }
    viewModel { OfflineRootsViewModel(get(), get(), get()) }

    viewModel { JobListViewModel(get()) }
    viewModel { LogListViewModel(get()) }

    viewModel { ChooseTargetViewModel(get()) }
    viewModel { PickSessionViewModel(get()) }
    viewModel { PickFolderViewModel(get(), get()) }

    viewModel { params -> TransferViewModel(params.get(), get(), get()) }
    viewModel { params -> TransferMenuViewModel(params.get(), params.get(), get()) }

    viewModel { params -> SearchViewModel(params.get(), get()) }

    viewModel { CarouselViewModel(get(), get()) }
}

val allModules = appModule + dbModule + daoModule + serviceModule + viewModelModule
