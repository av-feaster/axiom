package com.axiom.android.sdk.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.axiom.core.ModelManager
import com.axiom.android.sdk.ui.components.AxiomBottomNavigationBar
import com.axiom.android.sdk.ui.components.MyModelsBottomSheet
import com.axiom.android.sdk.domain.ModelUIItem
import com.axiom.android.sdk.viewmodel.BottomNavItem

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
}
