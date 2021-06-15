package Service;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class GoogleBillingImpl implements PurchasesUpdatedListener {
    private static final String TAG = "GoogleBillingImpl";

    private final BillingClient mBillingClient;
    private List<SkuDetails> skuDetailsList = new ArrayList<>();

    public GoogleBillingImpl(@NonNull final Context applicationContext)
    {
        mBillingClient = BillingClient.newBuilder(applicationContext)
                .enablePendingPurchases()
                .setListener(this)
                .build();
    }

    public void init()
    {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult)
            {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                {
                    // The billing client is ready. You can query purchases here.
                    List<String> strList = new ArrayList<>();
                    strList.add("test_1000");
                    strList.add("test_2000");

                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(strList).setType(BillingClient.SkuType.INAPP);

                    mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                         @Nullable List<SkuDetails> list)
                        {
                            // Process the result.
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                                    && list != null)
                            {
                                if(list.isEmpty())
                                {
                                    Log.d(TAG, "list is zero");
                                }else{
                                    skuDetailsList = list;
                                }
                            }
                        }
                    });
                }else{
                    Log.d(TAG, "google purchase error");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d(TAG, "onBillingServiceDisconnected");
            }
        });
    }

    public void purchase(Activity activity, String productId)
    {
        BillingFlowParams flowParams = null;
        BillingResult billingResult;

        SkuDetails sku = getSkuDetail(productId);
        if( sku != null )
        {
            flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(sku)
                    .build();
            billingResult = mBillingClient.launchBillingFlow(activity, flowParams);
        }
        else
        {
            Log.d(TAG, "sku is null");
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && list != null) {
            for (Purchase purchase : list) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(TAG, "user purchase cancel");
        } else {
            // Handle any other error codes.
            Log.d(TAG, "google purchase error");
        }
    }

    private void handlePurchase(Purchase purchase) {
        String purchaseToken, payLoad;
        purchaseToken = purchase.getPurchaseToken();
        payLoad = purchase.getDeveloperPayload();

        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
        {
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchaseToken)
                            .build();

            mBillingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s)
                {
                    if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                    {
                        Log.d(TAG, "google purchase success");
                    }else{
                        Log.d(TAG, "google purchase consume error");
                    }
                }
            });
        }
    }

    private SkuDetails getSkuDetail(String productId) {
        for(SkuDetails item : skuDetailsList) {
            if(item.getSku().equals(productId)) {
                return item;
            }
        }
        return null;
    }
}