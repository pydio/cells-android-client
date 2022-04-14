package com.pydio.android.cells.services

import androidx.room.Room
import androidx.work.WorkerParameters
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
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.account.AccountListViewModel
import com.pydio.android.cells.ui.auth.OAuthViewModel
import com.pydio.android.cells.ui.auth.P8CredViewModel
import com.pydio.android.cells.ui.auth.ServerUrlViewModel
import com.pydio.android.cells.ui.browse.BookmarksViewModel
import com.pydio.android.cells.ui.browse.BrowseFolderViewModel
import com.pydio.android.cells.ui.browse.OfflineRootsViewModel
import com.pydio.android.cells.ui.menus.TransferMenuViewModel
import com.pydio.android.cells.ui.menus.TreeNodeMenuViewModel
import com.pydio.android.cells.ui.search.SearchViewModel
import com.pydio.android.cells.ui.transfer.ChooseTargetViewModel
import com.pydio.android.cells.ui.transfer.PickFolderViewModel
import com.pydio.android.cells.ui.transfer.PickSessionViewModel
import com.pydio.android.cells.ui.transfer.TransferViewModel
import com.pydio.android.cells.ui.viewer.CarouselViewModel

val databaseModule = module {

    // Account DB and corresponding DAO instances
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            AccountDB::class.java,
            "accountdb"
        )
            .build()
    }
    single { get<AccountDB>().accountDao() }
    single { get<AccountDB>().authStateDao() }
    single { get<AccountDB>().legacyCredentialsDao() }
    single { get<AccountDB>().liveSessionDao() }
    single { get<AccountDB>().sessionDao() }
    single { get<AccountDB>().tokenDao() }
    single { get<AccountDB>().workspaceDao() }

    // Runtime DB
    single {
        Room.databaseBuilder(
            androidContext().applicationContext,
            RuntimeDB::class.java,
            "runtime_objects"
        ).build()
    }
    single { get<RuntimeDB>().transferDao() }

}

val serviceModule = module {

    // Manage network state
    single { NetworkService(androidContext()) }

    // Manage accounts and authentication
    single<Store<Server>>(named("ServerStore")) { MemoryStore<Server>() }
    single<Store<Transport>>(named("TransportStore")) { MemoryStore<Transport>() }
    single<Store<Token>>(named("TokenStore")) { TokenStore(get()) }
    single<Store<String>>(named("PasswordStore")) { PasswordStore(get()) }
    single { CredentialService(get(named("TokenStore")), get(named("PasswordStore"))) }
    single { AuthService(get()) }
    single { SessionFactory(get(), get(named("ServerStore")), get(named("TransportStore")), get()) }
    single<AccountService> { AccountServiceImpl(get(), get(), get()) }

    // Business services
    single { TreeNodeRepository(androidContext().applicationContext, get()) }
    single { NodeService(get(), get(), get()) }
    single { FileService(get()) }
    single { TransferService(get(), get(), get(), get()) }

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

    viewModel { ActiveSessionViewModel(get()) }
    viewModel { params -> BrowseFolderViewModel(params.get(), get()) }
    viewModel { params -> TreeNodeMenuViewModel(params.get(), params.get(), get()) }

    viewModel { BookmarksViewModel(get()) }
    viewModel { OfflineRootsViewModel(get()) }

    viewModel { ChooseTargetViewModel(get()) }
    viewModel { PickSessionViewModel(get()) }
    viewModel { PickFolderViewModel(get(), get()) }

    viewModel { TransferViewModel(get()) }
    viewModel { params -> TransferMenuViewModel(params.get(), get()) }

    viewModel { SearchViewModel(get()) }

    viewModel { CarouselViewModel(get()) }
}

val dbTestModule = module {
    single {
        // In-Memory database config
        Room.inMemoryDatabaseBuilder(get(), AccountDB::class.java)
            .allowMainThreadQueries()
            .build()
    }
}

val allModules = databaseModule + serviceModule + viewModelModule
