package com.example.billing

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val repository = ReminderRepository(database.reminderDao(), context)

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private var billingClient: BillingClient? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        // Load initial local status from cache repository 
        val cached = repository.getPrefBoolean("premium_subscribed", false)
        _isSubscribed.value = cached
        Log.d(TAG, "Initialized BillingManager. Cached status: $cached")
        
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        Log.d(TAG, "Initializing Google Play Billing Client...")
        try {
            val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()

            billingClient = BillingClient.newBuilder(context)
                .setListener(PurchasesUpdatedListener { billingResult, purchases ->
                    handlePurchasesUpdated(billingResult, purchases)
                })
                .enablePendingPurchases(pendingPurchasesParams)
                .build()

            connectToPlayBilling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BillingClient securely", e)
        }
    }

    private fun connectToPlayBilling() {
        try {
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    try {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Billing Client Setup Finished successfully.")
                            querySubscribedPurchases()
                            queryProductPlans()
                        } else {
                            Log.e(TAG, "Billing Client setup failed: ${billingResult.debugMessage}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inside onBillingSetupFinished callback", e)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing Service is disconnected.")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed startConnection", e)
        }
    }

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        try {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchaseIfNeeded(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i(TAG, "User canceled the purchase flow.")
            } else {
                Log.e(TAG, "Purchase update error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling updated purchases", e)
        }
    }

    private fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        try {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgeParams) { billingResult ->
                    try {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged successfully.")
                            updateSubscriptionState(true)
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inside acknowledge callback", e)
                    }
                }
            } else {
                updateSubscriptionState(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acknowledging purchase", e)
        }
    }

    fun updateSubscriptionState(active: Boolean) {
        _isSubscribed.value = active
        // Sync with persisted repo prefs
        coroutineScope.launch {
            try {
                repository.editPrefs {
                    putBoolean("premium_subscribed", active)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error editing subscription prefs", e)
            }
        }
    }

    private fun querySubscribedPurchases() {
        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                try {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val activeSub = purchases?.any { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && 
                            purchase.products.contains(PRODUCT_PREMIUM_MONTHLY)
                        } ?: false
                        updateSubscriptionState(activeSub)
                        Log.d(TAG, "Active subscription queried: $activeSub")
                    } else {
                        Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inside queryPurchasesAsync callback", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying subscribed purchases async", e)
        }
    }

    private fun queryProductPlans() {
        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_PREMIUM_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                try {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val premiumDetails = productDetailsList?.firstOrNull { it.productId == PRODUCT_PREMIUM_MONTHLY }
                        _productDetails.value = premiumDetails
                        Log.d(TAG, "Successfully query ProductDetails: $premiumDetails")
                    } else {
                        Log.e(TAG, "Failed querying product details: ${billingResult.debugMessage}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inside queryProductDetailsAsync callback", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying product plans async", e)
        }
    }

    /**
     * Launches the Google Play billing purchase flow or fires simulator mock fallback if disconnected.
     */
    fun purchaseSubscription(activity: android.app.Activity, onSuccessMock: (() -> Unit)? = null) {
        val details = _productDetails.value
        val client = billingClient

        if (client != null && client.isReady && details != null) {
            // Retrieve subscription offer details token
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            client.launchBillingFlow(activity, flowParams)
        } else {
            Log.w(TAG, "Billing Client not connected/ready. Triggering Mock Simulation instead.")
            // Simulated payment helper flow in sandbox environment
            coroutineScope.launch {
                // Instantly unlock Subscribed status in our sandbox repository
                updateSubscriptionState(true)
                onSuccessMock?.invoke()
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_PREMIUM_MONTHLY = "sub_premium_monthly"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
