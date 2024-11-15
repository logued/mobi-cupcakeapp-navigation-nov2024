/*
    CupcakeScreen -
    Starting point that contains AppBar and CupCakeApp composable
    CupCakeApp contains the NavHost which allows use to set up the navigation routes.
    The ViewModel and navController are instantiated here.

 */
package com.example.cupcake

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cupcake.data.DataSource
import com.example.cupcake.data.OrderUiState
import com.example.cupcake.ui.OrderSummaryScreen
import com.example.cupcake.ui.OrderViewModel
import com.example.cupcake.ui.SelectOptionScreen
import com.example.cupcake.ui.StartOrderScreen

/**
 * enum values that represent the screens in the app
 * In the NavHost, the name of the enum is extracted as a String,
 * and is used as the 'route' or destination screen.
 * (A route is a string that corresponds to a destination screen)
 */
enum class CupcakeScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Flavor(title = R.string.choose_flavor),
    Pickup(title = R.string.choose_pickup_date),
    Summary(title = R.string.order_summary)
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@Composable
fun CupcakeAppBar(
    currentScreen: CupcakeScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

@Composable
fun CupcakeApp(
    // the two parameters defined below are initialized with default values
    viewModel: OrderViewModel = viewModel(),    // create or retrieve existing ViewModel
    navController: NavHostController = rememberNavController()  // create

) {
    // Get current "back stack" entry as type State (if any)
    // i.e this is the currently displayed screen (as State)
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Get the name of the current screen as a String  (if null, set to "Start")
    val currentScreen = CupcakeScreen.valueOf(
        backStackEntry?.destination?.route ?: CupcakeScreen.Start.name
    )

    Scaffold(
        topBar = {
            CupcakeAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,  // generates a  Boolean
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->

        // uiState updated by flow from ViewModel
        // and observed by Compose
        val uiState by viewModel.uiState.collectAsState()
/////////////////////////////////////////////////////////////////////////////////////   NavHost
        NavHost(  // parameters supplied to navHost composable
            navController = navController,
            startDestination = CupcakeScreen.Start.name, // name of the enum i.e. "Start"
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {

            // add a list of composable screens and their identifiers to the NavHost
            // Each composable screen is identified by a String value (called a "route")
            //  - the route is simply a name for identifying a composable screen within the NavHost
            //  - screens are called Destinations
            //
            composable(route = CupcakeScreen.Start.name) {  // route is string representation of a destination
                StartOrderScreen(   // destination screen
                    // pass in any data that is required in the screen

                    // get list of [button name,quantity] Pairs
                    quantityOptions = DataSource.quantityOptions,

                    // add the lambda code to be called when user clicks on button to selecting
                    // quantity, and requesting move to next screen.
                    onNextButtonClicked = {
                        // a lambda block of code that receives one argument referred to as "it"
                        // that is the quantity of cupcakes required (as per button clicked).
                        // The quantity is passed into the ViewModel by a call to setQuantity,
                        // and it updates the quantity (and price) in the uiState.
                        viewModel.setQuantity(it) // update state

                        // Note: in general, we call functions in the ViewModel to update the _uiState
                        // because the state is private. We don't allow direct access the state directly
                        // as this is good OOP 'encapsulation' practice, and reduces chances of spurious
                        // updates of state variables.

                        // Next, navigate() is called to navigate to the next screen
                        // identified by its route (i.e a String representation of it name)
                        navController.navigate(CupcakeScreen.Flavor.name) // move to destination screen
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
            composable(route = CupcakeScreen.Flavor.name) {
                val context = LocalContext.current
                SelectOptionScreen(
                    subtotal = uiState.price,
                    onNextButtonClicked = { navController.navigate(CupcakeScreen.Pickup.name) },
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                    },
                    // map = iterate over the list of resource IDs,
                    //        and transform (map) from id into corresponding String
                    // (map applies the transform function to all items in list )
                    //  Current local Context is required.
                    options = DataSource.flavors.map { id -> context.resources.getString(id) },
                    onSelectionChanged = { viewModel.setFlavor(it) },
                    modifier = Modifier.fillMaxHeight()
                )
        }
            composable(route = CupcakeScreen.Pickup.name) {
                // Notice that the SelectOptionScreen composable function is used to
                // create both the Flavor screen (above) and the Pickup date screen.
                //
                SelectOptionScreen(
                    subtotal = uiState.price,
                    onNextButtonClicked = { navController.navigate(CupcakeScreen.Summary.name) },
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                    },
                    options = uiState.pickupOptions,
                    onSelectionChanged = { viewModel.setDate(it) },  // set Pickup date
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable(route = CupcakeScreen.Summary.name) {
                val context = LocalContext.current
                OrderSummaryScreen(
                    orderUiState = uiState,
                    onCancelButtonClicked = {
                        cancelOrderAndNavigateToStart(viewModel, navController)
                    },
                    onSendButtonClicked = {
                        subject: String,
                        summary: String -> shareOrder(context, subject = subject, summary = summary)
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Resets the [OrderUiState] and pops up to [CupcakeScreen.Start]
 */
private fun cancelOrderAndNavigateToStart(
    viewModel: OrderViewModel,
    navController: NavHostController
) {
    viewModel.resetOrder()
    navController.popBackStack(CupcakeScreen.Start.name, inclusive = false)
}

/**
 * Creates an intent to share order details with/using some other activity
 */
private fun shareOrder(context: Context, subject: String, summary: String) {
    // Create an ACTION_SEND implicit intent with order details in the intent extras
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    // Need to use 'context' - which is the component that is trying to start another activity.
    // Android needs to know where the request originated from, and this is the context.
    // Context also needed to access resource id.
    //
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.new_cupcake_order)
        )
    )
}
