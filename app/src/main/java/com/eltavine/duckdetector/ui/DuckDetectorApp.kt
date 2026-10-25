/*
 * Copyright 2026 Duck Apps Contributor
 * If you have any questions, suggestions, or other inquiries, please email Eltavine <me@eltavine.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eltavine.duckdetector.core.detector.ConsentDecision
import com.eltavine.duckdetector.core.detector.ConsentId
import com.eltavine.duckdetector.notifications.ScanNotificationPermissions
import com.eltavine.duckdetector.notifications.preferences.ScanNotificationConsentStore
import com.eltavine.duckdetector.packagevisibility.preferences.PackageVisibilityReviewStore
import com.eltavine.duckdetector.startup.StartupPolicyReviewStore
import com.eltavine.duckdetector.startup.legal.AgreementAcceptanceStore
import com.eltavine.duckdetector.ui.shell.AppDestination
import com.eltavine.duckdetector.ui.shell.combineConsentDecisions
import io.github.xiaotong6666.uihelper.mode.UiMode

@Composable
fun DuckDetectorApp(onUiModeChange: (UiMode) -> Unit) {
    val blacklistMatch = remember { DeviceBlacklist.matchCurrentDevice() }
    if (blacklistMatch != null) {
        Surface {
            BlockedDeviceScreen(
                match = blacklistMatch,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    val context = LocalContext.current
    val appContext = context.applicationContext
    val agreementStore = remember(appContext) { AgreementAcceptanceStore.getInstance(appContext) }
    val notificationConsentStore = remember(appContext) {
        ScanNotificationConsentStore.getInstance(appContext)
    }
    val startupPolicyReviewStore = remember(appContext) {
        StartupPolicyReviewStore.getInstance(appContext)
    }
    val packageVisibilityReviewStore = remember(appContext) {
        PackageVisibilityReviewStore.getInstance(appContext)
    }

    LaunchedEffect(Unit) {
        agreementStore.accept()
        startupPolicyReviewStore.complete()
        packageVisibilityReviewStore.acknowledgeRestrictedInventory()
        notificationConsentStore.markNotificationsPrompted()
        notificationConsentStore.markLiveUpdatesPrompted()
    }

    val consentDecisions by produceState<Map<ConsentId, ConsentDecision>?>(
        initialValue = null,
        key1 = appContext,
    ) {
        combineConsentDecisions(
            DetectorFeatures.consentCards.associate { it.consent.id to it.consent.decisions(appContext) },
        ).collect { decisions ->
            value = decisions
        }
    }

    val notificationPermissionState by remember {
        mutableStateOf(ScanNotificationPermissions.read(appContext))
    }

    var destination by rememberSaveable { mutableStateOf(AppDestination.MAIN) }

    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            val resolvedConsentDecisions = consentDecisions
            if (resolvedConsentDecisions == null) {
                StartupBootstrapLoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                AppReadyShell(
                    destination = destination,
                    onSelectDestination = { selected -> destination = selected },
                    onUiModeChange = onUiModeChange,
                    consentDecisions = resolvedConsentDecisions,
                    notificationPermissionState = notificationPermissionState,
                )
            }
        }
    }
}
