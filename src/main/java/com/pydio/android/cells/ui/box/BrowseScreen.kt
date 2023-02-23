package com.pydio.android.cells.ui.box

private const val logTag = "BrowseScreen.kt"

//@Composable
//fun BrowseScreen(
//    initialStateID: StateID,
//    postActivity: (stateID: StateID, action: String?) -> Unit,
//    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
//    accountListVM: AccountListVM = koinViewModel(),
//) {
//    val ctx = LocalContext.current
//
//    val navController = rememberNavController()
//
//    val currLoadingState by browseRemoteVM.loadingState.observeAsState()
//
//    val startDestination = when {
////        initialStateID == Transport.UNDEFINED_STATE_ID
////        -> BrowseDestination.ChooseAccount.route
//        Str.empty(initialStateID.workspace)
////        -> BrowseDestination.AccountHome.createRoute(initialStateID.account())
//        -> BrowseDestinations.AccountHome.route
//        else
//        -> BrowseDestinations.Open.createRoute(initialStateID)
//    }
//
//    val openDrawer: (StateID) -> Unit = {
//
//    }
//
//    val open: (StateID) -> Unit = { stateID ->
//        navController.navigate(
//            if (Str.notEmpty(stateID.workspace)) {
//                BrowseDestinations.Open.createRoute(stateID)
//            } else {
//                BrowseDestinations.AccountHome.route
//            }
//        )
//    }
//
//    val openParent: (StateID) -> Unit = { stateID ->
//        val parent = stateID.parent()
//        open(parent)
//    }
//
//    // val openAccounts: () -> Unit = { navController.navigate(BrowseDestination.ChooseAccount.route) }
//    val openAccounts: () -> Unit = { throw RuntimeException("Implement me") }
//
//    NavHost(
//        navController = navController, startDestination = startDestination
//    ) {
//
////composable(BrowseDestination.ChooseAccount.route) {
////            Log.d(logTag, ".... Open choose account page")
////            // TODO double check this might not be called on the second pass
////            LaunchedEffect(true) {
////                Log.e(logTag, ".... Choose account, launching effect")
////                accountListVM.watch()
////                browseRemoteVM.pause()
////            }
////
////            SelectAccount(
////                accountListVM = accountListVM,
////                openAccount = open,
////                back = {},
////                registerNew = {},
////                login = {},
////                logout = {},
////                forget = {},
////            )
////        }
//
////        composable(BrowseDestination.AccountHome.route) { navBackStackEntry ->
////            val stateId =
////                navBackStackEntry.arguments?.getString(BrowseDestination.AccountHome.getPathKey())
////                    ?: initialStateID.id
////            Log.e(logTag, ".... Open account home with ID: ${StateID.fromId(stateId)}")
////
////            AccountHome(
////                StateID.fromId(stateId),
////                openDrawer = openDrawer,
////                openAccounts = openAccounts,
////                openSearch = {},
////                openWorkspace = open,
////                browseRemoteVM = browseRemoteVM,
////            )
////        }
////
////        composable(BrowseDestination.OpenFolder.route) { navBackStackEntry ->
////            val stateId =
////                navBackStackEntry.arguments?.getString(BrowseDestination.AccountHome.getPathKey())
////                    ?: initialStateID.id
////            Log.e(logTag, ".... Open folder at ${StateID.fromId(stateId)}")
////
////            Folder(
////                StateID.fromId(stateId),
////                openDrawer = openDrawer,
////                openParent = openParent,
////                open = open,
////                openSearch = {},
////                browseRemoteVM = browseRemoteVM,
////            )
////        }
//    }
//}
//
//@Composable
//fun BrowseApp(content: @Composable () -> Unit) {
//    CellsTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
//        ) {
//            content()
//        }
//    }
//}
