package com.sentinela.camtv.ui.app

class AppNavigator(
    initialState: AppUiState = AppUiState(),
) {
    var state: AppUiState = initialState
        private set

    private var returnDestination: AppDestination = AppDestination.Home

    fun initialize(hasCameras: Boolean) {
        state = AppUiState(
            destination = if (hasCameras) AppDestination.Mosaic else AppDestination.Home,
            hasCameras = hasCameras,
        )
        returnDestination = AppDestination.Home
    }

    fun setCameraAvailability(hasCameras: Boolean) {
        state = state.copy(hasCameras = hasCameras)
    }

    fun openHome() {
        state = state.copy(destination = AppDestination.Home)
        returnDestination = AppDestination.Home
    }

    fun openMosaic() {
        state = state.copy(destination = AppDestination.Mosaic)
        returnDestination = AppDestination.Home
    }

    fun openCameras() {
        openReturnableDestination(AppDestination.Cameras)
    }

    fun openCaptures() {
        openReturnableDestination(AppDestination.Captures)
    }

    fun openSettings() {
        openReturnableDestination(AppDestination.Settings)
    }

    fun goBack() {
        state = state.copy(destination = resolvedReturnDestination())
        returnDestination = AppDestination.Home
    }

    private fun openReturnableDestination(destination: AppDestination) {
        returnDestination = when (state.destination) {
            AppDestination.Mosaic,
            AppDestination.Home -> state.destination

            AppDestination.Cameras,
            AppDestination.Captures,
            AppDestination.Settings,
            AppDestination.Loading -> AppDestination.Home
        }
        state = state.copy(destination = destination)
    }

    private fun resolvedReturnDestination(): AppDestination = returnDestination
}
