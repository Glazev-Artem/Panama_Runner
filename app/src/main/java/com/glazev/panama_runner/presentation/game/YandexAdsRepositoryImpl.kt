package com.glazev.panama_runner.presentation.game

import android.content.Context
import com.glazev.panama_runner.AdsConfig
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class YandexAdsRepositoryImpl(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private val rewardedAdLoader: RewardedAdLoader = RewardedAdLoader(context)

    init {
        // 1. Инициализация SDK
        MobileAds.initialize(context) {
            loadRewardedVideo()
        }

        // 2. Настройка лоадера
        rewardedAdLoader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(rewarded: RewardedAd) {
                rewardedAd = rewarded
            }
            override fun onAdFailedToLoad(error: com.yandex.mobile.ads.common.AdRequestError) {
                // Логика перезагрузки через время
            }
        })
    }

    private fun loadRewardedVideo() {
        val adRequestConfiguration = AdRequestConfiguration.Builder(AdsConfig.REWARDED_ID).build()
        rewardedAdLoader.loadAd(adRequestConfiguration)
    }

    fun showVideo(activity: android.app.Activity, onReward: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(adError: com.yandex.mobile.ads.common.AdError) {
                    rewardedAd = null
                    loadRewardedVideo()
                }
                override fun onAdDismissed() {
                    rewardedAd = null
                    loadRewardedVideo()
                }
                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: com.yandex.mobile.ads.common.ImpressionData?) {}
                override fun onRewarded(reward: com.yandex.mobile.ads.rewarded.Reward) {
                    onReward() // Выдача награды
                }
            })
            ad.show(activity)
        } ?: run {
            loadRewardedVideo()
        }
    }
}
