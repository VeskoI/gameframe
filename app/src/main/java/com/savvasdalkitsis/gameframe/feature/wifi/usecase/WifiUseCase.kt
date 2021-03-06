/**
 * Copyright 2017 Savvas Dalkitsis
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 'Game Frame' is a registered trademark of LEDSEQ
 */
package com.savvasdalkitsis.gameframe.feature.wifi.usecase

import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.savvasdalkitsis.gameframe.feature.ip.model.IpAddress
import io.reactivex.Single

class WifiUseCase(private val wifiManager: WifiManager) {

    fun isWifiEnabled(): Single<Boolean> = Single.just(wifiManager.isWifiEnabled)

    fun enableWifi() = wifiManager.setWifiEnabled(true)

    @Suppress("DEPRECATION")
    fun getDeviceIp(): Single<IpAddress> = Single.defer {
        val ip = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        Single.just(IpAddress.parse(ip))
    }
}