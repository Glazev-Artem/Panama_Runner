package com.glazev.panama_runner.presentation.game

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class AdsManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private val rewardedAdLoader: RewardedAdLoader = RewardedAdLoader(context)

    init {
        rewardedAdLoader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                // В реальном приложении здесь стоит добавить лог или ретрай
            }
        })
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun loadRewardedVideo() {
        // Используем демо-ID для тестирования
        val adRequestConfiguration = AdRequestConfiguration.Builder("demo-rewarded-yandex").build()
        rewardedAdLoader.loadAd(adRequestConfiguration)
    }

    fun showRewardedVideo(
        activity: Activity,
        onAdStarted: () -> Unit,
        onRewardGranted: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var isRewarded = false
            ad.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {
                    onAdStarted()
                }
                override fun onAdFailedToShow(adError: AdError) {
                    rewardedAd = null
                    loadRewardedVideo()
                }
                override fun onAdDismissed() {
                    val wasRewarded = isRewarded
                    rewardedAd = null
                    if (wasRewarded) {
                        onRewardGranted()
                    }
                    loadRewardedVideo()
                }
                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
                override fun onRewarded(reward: Reward) {
                    isRewarded = true
                }
            })
            ad.show(activity)
        } else {
            loadRewardedVideo()
        }
    }

    fun isAdLoaded(): Boolean = rewardedAd != null
}
