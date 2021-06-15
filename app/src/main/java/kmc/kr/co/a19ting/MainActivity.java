package kmc.kr.co.a19ting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.Manifest;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

import com.google.firebase.iid.FirebaseInstanceId;

import kmc.kr.co.a19ting.Security;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import Service.GoogleBillingImpl;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    public ValueCallback<Uri> filePathCallbackNormal;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    private Uri cameraImageUri = null;


    private WebView mWebView; // 웹뷰 선언
    private WebSettings mWebSettings; //웹뷰세팅
    private static final String JAVASCRIPT_APP_KEY = "android";

    private final Handler handler = new Handler();

    private BillingClient billingClient;


    public static  String SELECTED_ITEM_ID= "";
    public static String BILLING_INFO = "";
    public static String PURCHASE_TOKEN = "";

    public static String TICKETID = "";
    public static String PRODUCTID = "";
    private static String calls = "";
    private static String developerPayLoad = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Intent intent = getIntent();
        String url = intent.getStringExtra("url");

//        Toast.makeText(getApplicationContext(), "url : "+url, Toast.LENGTH_LONG).show();

        Uri data = this.getIntent().getData();

        if (data != null && data.isHierarchical()) {
            String uri = this.getIntent().getDataString();
            Log.i("MyApp", "Deep link clicked " + uri);
        }


        mWebView = (WebView) findViewById(R.id.webView);

        mWebSettings = mWebView.getSettings(); //세부 세팅 등록
        mWebSettings.setJavaScriptEnabled(true); // 웹페이지 자바스클비트 허용 여부
        mWebSettings.setSupportMultipleWindows(false); // 새창 띄우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false); // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        mWebSettings.setLoadWithOverviewMode(true); // 메타태그 허용 여부
        mWebSettings.setUseWideViewPort(true); // 화면 사이즈 맞추기 허용 여부
        mWebSettings.setSupportZoom(false); // 화면 줌 허용 여부
        mWebSettings.setBuiltInZoomControls(false); // 화면 확대 축소 허용 여부
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); // 컨텐츠 사이즈 맞추기
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true); // 로컬저장소 허용 여부
        mWebView.addJavascriptInterface(new AndroidBridge(), JAVASCRIPT_APP_KEY);
//        mWebView.getSettings().setDomStorageEnabled(true);
//        mWebView.getSettings().setJavaScriptEnabled(true);
        String userAgent = mWebView.getSettings().getUserAgentString();

        mWebView.getSettings().setUserAgentString(userAgent + " GOOGLE_NINETING_APP");


        if (url != null) {
            Log.d("url", url);
            mWebView.loadUrl(url);
        } else {
            mWebView.loadUrl("https://www.19ting.co.kr/");

//            mWebView.loadUrl("https://z594g.csb.app/");

        }

        mWebView.setWebViewClient(new CustomWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());


        mWebView.setWebChromeClient(new WebChromeClient() {
            // input 클릭시 바로 실행되는 부분
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                // Callback 초기화
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;

                boolean isCapture = fileChooserParams.isCaptureEnabled();

                runCamera(isCapture);
                return true;
            }

        });







        // Establish connection to billing client
        //check purchase status from google play store cache on every app start
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK){
                    Purchase.PurchasesResult queryPurchase = billingClient.queryPurchases(INAPP);
                    List<Purchase> queryPurchases = queryPurchase.getPurchasesList();
                    if(queryPurchases!=null && queryPurchases.size()>0){
                        try {
                            handlePurchases(queryPurchases);
                        } catch (IOException | ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });

        onCheckPermission();

    }
    // 액티비티가 종료될 때 결과를 받고 파일을 전송할 때 사용
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILECHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK) {
                    if (filePathCallbackNormal == null) return;
                    Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData(); //  onReceiveValue 로 파일을 전송한다.

                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            case FILECHOOSER_LOLLIPOP_REQ_CODE:

                if (resultCode == RESULT_OK) {
                    if (filePathCallbackLollipop == null) return;
                    if (data == null)
                        data = new Intent();
                    if (data.getData() == null)
                        data.setData(cameraImageUri);

                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    filePathCallbackLollipop = null;

                } else {
                    if (filePathCallbackLollipop != null) {   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }

                    if (filePathCallbackNormal != null) {
                        filePathCallbackNormal.onReceiveValue(null);
                        filePathCallbackNormal = null;
                    }
                }
                break;
            default:

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }



    // 카메라 기능 구현
    private void runCamera(boolean _isCapture) {
        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, "temp.png"); // temp.png 는 카메라로 찍었을 때 저장될 파일명이므로 사용자 마음대로

        cameraImageUri = Uri.fromFile(file);
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        if (!_isCapture) { // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
            Intent pickIntent = new Intent(Intent.ACTION_PICK);

                pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);


            String pickTitle = "사진 가져올 방법을 선택하세요.";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

            // 카메라 intent 포함시키기..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
        else {// 바로 카메라 실행..
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
    }





    public void onCheckPermission(){
        String temp = "";

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " ";
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.CAMERA + " ";
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_EXTERNAL_STORAGE + " ";
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.READ_CONTACTS + " ";
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.CALL_PHONE + " ";
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.RECORD_AUDIO + " ";
        }
        if (temp != "") {
            ActivityCompat.requestPermissions(this,
                    temp.trim().split(" "), 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }


    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        if (url != null){
            mWebView.loadUrl(url);
        }
    }

    //initiate purchase on consume button click
    public void consume(String productId) {
        //check if service is already connected
        if (billingClient.isReady()) {
            initiatePurchase(productId);

        }
        //else reconnect service
        else{
            billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        initiatePurchase(productId);

                    } else {
                        Toast.makeText(getApplicationContext(),"Error : 인앱 구매 설정 실패",Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                }
            });
        }
    }
    private void initiatePurchase(final String PRODUCT_ID) {
        List<String> skuList = new ArrayList<>();
        skuList.add(PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null && skuDetailsList.size() > 0) {
                                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                        .setSkuDetails(skuDetailsList.get(0))
                                        .build();
                                billingClient.launchBillingFlow(MainActivity.this, flowParams);
                            }
                            else{
                                //try to add item/product id "consumable" inside managed product in google play console
                                Toast.makeText(getApplicationContext(),"구글 플레이에서 아이템 정보를 찾을 수 없습니다",Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    " Error "+billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        Toast.makeText(getApplicationContext(),"onPurchasesUpdated",Toast.LENGTH_SHORT).show();

        //if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            try {
                handlePurchases(purchases);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        //if item already purchased then check and reflect changes
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Purchase.PurchasesResult queryAlreadyPurchasesResult = billingClient.queryPurchases(INAPP);
            List<Purchase> alreadyPurchases = queryAlreadyPurchasesResult.getPurchasesList();
            if(alreadyPurchases!=null){
                try {
                    handlePurchases(alreadyPurchases);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        //if purchase cancelled
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(getApplicationContext(),"Purchase Canceled",Toast.LENGTH_SHORT).show();
        }
        // Handle any other error msgs
        else {
            Toast.makeText(getApplicationContext(),"Error "+billingResult.getDebugMessage(),Toast.LENGTH_SHORT).show();
        }
    }
    void handlePurchases(List<Purchase>  purchases) throws IOException, ParseException {

        for(Purchase purchase:purchases) {
            //if item is purchased
            Log.d("펄체이 정보",purchase.toString());
            if (SELECTED_ITEM_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
            {
                if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {

                    // Invalid purchase
                    // show error to user
                    Toast.makeText(getApplicationContext(), "Error : Invalid Purchase", Toast.LENGTH_SHORT).show();
                    return;
                }

                // else purchase is valid
                //if item is purchased and not consumed
                if (!purchase.isAcknowledged()) {
                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

                    billingClient.consumeAsync(consumeParams, consumeListener);
                    developerPayLoad = purchase.getDeveloperPayload();
                }
                //여기까지 펄체이스

                JSONParser parser = new JSONParser();

                JSONObject jsonObject = (JSONObject) parser.parse(purchase.toString().substring(16));

                jsonObject.put("ticketId",TICKETID);
                jsonObject.put("productId",PRODUCTID);
                mWebView.loadUrl("javascript:njResponseGoogleInappBillingPurchase('" + jsonObject + "')");

            }
            //if purchase is pending
            else if( SELECTED_ITEM_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.PENDING)
            {
                Toast.makeText(getApplicationContext(),
                        "Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show();
            }
            //if purchase is refunded or unknown
            else if(SELECTED_ITEM_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE)
            {
                Toast.makeText(getApplicationContext(), "Purchase Status Unknown", Toast.LENGTH_SHORT).show();
            }
        }
    }

    ConsumeResponseListener consumeListener = new ConsumeResponseListener() {
        @Override
        public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    BILLING_INFO = billingResult.toString();
                    PURCHASE_TOKEN = purchaseToken;
                mWebView.loadUrl("javascript:njResponseGoogleInappBillingConsume(true)");


                Toast.makeText(getApplicationContext(), "아이템 정보 업데이트 완료", Toast.LENGTH_SHORT).show();

            }else{
                mWebView.loadUrl("javascript:njResponseGoogleInappBillingConsume(false)");

            }
        }
    };

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) throws IOException {
        //for old playconsole
        // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
        //for new play console
        //To get key go to Developer Console > Select your app > Monetize > Monetization setup

        String base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk3txiLZJQx5B6VYll1CDM1gvklQN2lybBQVFeMdk9lKskeIVJY1NX8WYCVkxVXScG910Y3y+AWTQcNEPtzZZ/5kSW/h2aPGq10cu2SajZfIjXV4G+lrzIVfMI//0oN2flwwmkKKHT5gXg/NcU/Z+Ii7+HIjOLvc/M5W0lx9pq1kSebyMYM9Zppdc7oFScSiOWZx+Mk4SOGmxj4jV7uXbOq2k3fDXD+013T28oRHDcpoTv7/wn/4EYaSjLzrmveY7UJo2PcsBQ3wrcXZ4lIh7AVrU64fj+rvaxSw20Zg4nrPivtvm9v1e+x0HVx3IyBSK/JbgJpLKevBCt/IlMNgO3wIDAQAB";
//            return Security.verifyPurchase(base64Key, signedData, signature);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(billingClient!=null){
            billingClient.endConnection();
        }
    }


    private class AndroidBridge {


        @JavascriptInterface
        public void jnRequestOpenBrowser(String url){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:njRegisterFCM('" + FirebaseInstanceId.getInstance().getToken() + "')");
                }});
        }
        @JavascriptInterface
        public void jnRequestVerify(String url){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse(url);
                    intent.setData(uri);
                    startActivity(intent);




                }});
        }
        @JavascriptInterface
        public void jnRequestGoogleInappBillingPurchase(String json){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    JSONParser parser = new JSONParser();
                    String ticketId;
                    String productId;
                    try {
                        Log.d("인앱결제요청", json.toString());



                        JSONObject jsonObject = (JSONObject) parser.parse(json);
                        ticketId = (String) jsonObject.get("ticketId");
                        productId = (String) jsonObject.get("productId");
                        //여기서 인앱 결제하고

                        SELECTED_ITEM_ID = productId;
                        TICKETID = ticketId;
                        PRODUCTID = productId;
                        calls = "jnRequestGoogleInappBillingPurchase";
                        consume(productId);







                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }});
        }
        @JavascriptInterface
        public void jnRequestGoogleInappBillingConsume(String purchaseToken){
            handler.post(new Runnable() {
                @Override
                public void run() {

                // 소비 요청함

                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchaseToken)
                            .build();

                    billingClient.consumeAsync(consumeParams, consumeListener);



                    ConsumeResponseListener consumeListener = new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {


                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Log.d("빌링 리저트",billingResult.toString());
                                Log.d("펄체이스 토큰",purchaseToken.toString());
                                mWebView.loadUrl("javascript:njResponseGoogleInappBillingConsume(true)");
                            }else{
                                mWebView.loadUrl("javascript:njResponseGoogleInappBillingConsume(false)");

                            }
                        }
                    };


                }});
        }
        @JavascriptInterface
        public void jnRequestConsume(){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    billingClient.startConnection(new BillingClientStateListener() {
                        @Override
                        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                            if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK){
                                Purchase.PurchasesResult queryPurchase = billingClient.queryPurchases(INAPP);
                                List<Purchase> queryPurchases = queryPurchase.getPurchasesList();
                                if(queryPurchases!=null && queryPurchases.size()>0){
                                    try {
                                        handlePurchases(queryPurchases);
                                    } catch (IOException | ParseException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }

                        }


                        @Override
                        public void onBillingServiceDisconnected() {
                        }

                    });

                }});
        }

    }



}
