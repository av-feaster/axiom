package com.axiom.android.sdk.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.axiom.core.ModelManager
import com.axiom.android.sdk.ui.components.AxiomBottomNavigationBar
import com.axiom.android.sdk.ui.components.MyModelsBottomSheet
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.viewmodel.BottomNavItem
import com.axiom.android.sdk.ui.screens.DownloadsScreen

/**
 * Main entry point for Axiom UI components.
 * Provides Play Store-like model management UI.
 */
object AxiomUI {
    
    /**
     * Composable that displays the My Models bottom sheet.
     * 
     * @param models List of models to display
     * @param onImportLocalModel Callback when import local model is clicked
     * @param onOptimizeStorage Callback when optimize storage is clicked
     * @param onModelPauseResume Callback when pause/resume is clicked for a model
     * @param onDismiss Callback when the bottom sheet is dismissed
     * @param modifier Modifier for the component
     */
    @Composable
    fun MyModelsSheet(
        models: List<ModelUIItem>,
        onImportLocalModel: () -> Unit = {},
        onOptimizeStorage: () -> Unit = {},
        onModelPauseResume: (String) -> Unit = {},
        onDismiss: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        MyModelsBottomSheet(
            models = models,
            onImportLocalModel = onImportLocalModel,
            onOptimizeStorage = onOptimizeStorage,
            onModelPauseResume = onModelPauseResume,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
    
    /**
     * Composable that displays the bottom navigation bar.
     * 
     * @param selectedItem Currently selected tab
     * @param onItemSelected Callback when a tab is selected
     * @param modifier Modifier for the component
     */
    @Composable
    fun BottomNavigation(
        selectedItem: BottomNavItem,
        onItemSelected: (BottomNavItem) -> Unit,
        modifier: Modifier = Modifier
    ) {
        AxiomBottomNavigationBar(
            selectedItem = selectedItem,
            onItemSelected = onItemSelected,
            modifier = modifier
        )
    }

    /**
     * Composable that displays the Downloads screen.
     *
     * @param models List of models to display. For live download rows, build from
     * [com.axiom.android.sdk.models.AxiomModelManager.getAvailableModels] and merge in-flight state with
     * [com.axiom.android.sdk.domain.withActiveDownload] using
     * [com.axiom.android.sdk.models.AxiomModelManager.getActiveDownloadFlow] (same pattern as [com.axiom.android.sdk.ui.screens.ModelHubScreen]).
     * @param onPauseDownload Callback when pause is clicked for a download
     * @param onResumeDownload Callback when resume is clicked for a download
     * @param onCancelDownload Callback when cancel is clicked for a download
     * @param onDeleteModel Callback when delete is clicked for a model
     * @param onNavigateToStore Callback when navigating to store
     * @param onNavigateToMyModels Callback when navigating to my models
     * @param modifier Modifier for the component
     */
    @Composable
    fun DownloadsManager(
        models: List<ModelUIItem>,
        onPauseDownload: (String) -> Unit = {},
        onResumeDownload: (String) -> Unit = {},
        onCancelDownload: (String) -> Unit = {},
        onDeleteModel: (String) -> Unit = {},
        onNavigateToStore: () -> Unit = {},
        onNavigateToMyModels: () -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        DownloadsScreen(
            models = models,
            onPauseDownload = onPauseDownload,
            onResumeDownload = onResumeDownload,
            onCancelDownload = onCancelDownload,
            onDeleteModel = onDeleteModel,
            onNavigateToStore = onNavigateToStore,
            onNavigateToMyModels = onNavigateToMyModels,
            modifier = modifier
        )
    }
}
