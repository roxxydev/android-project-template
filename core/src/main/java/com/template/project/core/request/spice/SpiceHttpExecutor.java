package com.template.project.core.request.spice;

import android.content.Context;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.SpiceRequest;
import com.octo.android.robospice.request.listener.RequestListener;

/**
 * Makes http request asynchronous by using the NetworkHttpCallback.
 */
public class SpiceHttpExecutor {

    private static SpiceHttpExecutor sNetworkCallExecutor;

    private SpiceManager spiceManager;

    public static SpiceHttpExecutor getInstance() {
        if (sNetworkCallExecutor == null) {
            sNetworkCallExecutor = new SpiceHttpExecutor();
        }
        return sNetworkCallExecutor;
    }

    public interface NetworkHttpCallback {

        Object doExecution();

        /** Event when http request failed. */
        void onRequestFailure();

        /** Event when http request is in progress. */
        void onRequestProgress(float progress);

        /** Event when http request has response */
        void onRequestResponse(Object responseObj);
    }

    protected class NetworkHttpListener implements RequestListener<Object> {

        NetworkHttpCallback netHttpCallback;

        public NetworkHttpListener(NetworkHttpCallback networkHttpCallback) {
            this.netHttpCallback = networkHttpCallback;
        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            netHttpCallback.onRequestFailure();
        }

        @Override
        public void onRequestSuccess(Object responseObj) {
            netHttpCallback.onRequestResponse(responseObj);
        }
    }

    protected class NetworkRequest extends SpiceRequest<Object> {

        private NetworkHttpCallback netHttpCallback;

        public NetworkRequest(NetworkHttpCallback networkHttpCallback) {
            super(Object.class);
            this.netHttpCallback = networkHttpCallback;
        }

        @Override
        public Object loadDataFromNetwork() throws Exception {
            return netHttpCallback.doExecution();
        }

        @Override
        protected void publishProgress(float progress) {
            super.publishProgress(progress);
            netHttpCallback.onRequestProgress(progress);
        }
    }

    /** Execute http call on Robospice context to avoid network call on main thread. */
    public void executeHttpCall(Context context, NetworkHttpCallback networkHttpCallback) {
        if (spiceManager == null) {
            spiceManager = new SpiceManager(SpiceHttpService.class);
        }
        if (spiceManager != null) {
            NetworkRequest networkRequest = new NetworkRequest(networkHttpCallback);
            NetworkHttpListener netHttpListener = new NetworkHttpListener(networkHttpCallback);

            if (!spiceManager.isStarted()) {
                spiceManager.start(context);
            }
            spiceManager.execute(networkRequest, netHttpListener);
        }
    }

    /**
     * Release all network calls performed by Robospice. This can/should be called onDestroy
     * or onStop of the Activity/Fragment.
     */
    public void releaseNetworkCalls() {
        if (spiceManager != null && spiceManager.isStarted()) {
            spiceManager.cancelAllRequests();
            spiceManager.dontNotifyAnyRequestListeners();
            spiceManager.shouldStop();
        }
    }

}
