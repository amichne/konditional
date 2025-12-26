package io.amichne.konditional.configstate.ui

import io.amichne.konditional.configstate.ui.components.BindingsTable
import io.amichne.konditional.configstate.ui.components.DescriptorCatalog
import io.amichne.konditional.configstate.ui.components.EditableFieldsPanel
import io.amichne.konditional.configstate.ui.components.SnapshotJsonPanel
import io.amichne.konditional.configstate.ui.model.ConfigurationStateResponseDto
import io.amichne.konditional.configstate.ui.net.ConfigStateApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mui.material.Alert
import mui.material.Box
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Container
import mui.material.CssBaseline
import mui.material.CircularProgress
import mui.material.Divider
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.useEffect
import react.useState
import web.cssom.px

val ConfigStateCatalogApp: FC<Props> =
    FC {
        val (remoteState, setRemoteState) =
            useState<RemoteData<ConfigurationStateResponseDto>>(RemoteData.Loading)
        val (reloadToken, setReloadToken) = useState(0)

        useEffect(reloadToken) {
            val scope = MainScope()
            setRemoteState(RemoteData.Loading)
            scope.launch {
                val result = ConfigStateApi.fetchConfigurationState()
                setRemoteState(
                    result.fold(
                        onSuccess = { RemoteData.Loaded(it) },
                        onFailure = { RemoteData.Failed(it.message ?: "Unknown error") },
                    ),
                )
            }

            ({ scope.cancel() })
        }

        CssBaseline()

        Container {
            sx {
                paddingTop = 24.px
                paddingBottom = 48.px
            }

            Stack {
                Typography {
                    +"Konditional ConfigState UI Catalog"
                }

                Typography {
                    +"Renders editors from SupportedValues + bindings, and shows live snapshot traversal via JSON pointers."
                }

                Divider()

                Box {
                    Button {
                        variant = ButtonVariant.contained
                        onClick = { setReloadToken { it + 1 } }
                        +"Reload from /api/configstate"
                    }
                }

                when (val state = remoteState) {
                    RemoteData.Idle -> Unit
                    RemoteData.Loading -> {
                        Stack {
                            CircularProgress()
                            Typography { +"Loading…" }
                        }
                    }

                    is RemoteData.Failed -> {
                        Alert {
                            severity = "error"
                            Typography { +state.message }
                        }
                    }

                    is RemoteData.Loaded -> {
                        val response = state.value

                        BindingsTable {
                            bindings = response.supportedValues.bindings
                        }

                        DescriptorCatalog {
                            byType = response.supportedValues.byType
                        }

                        EditableFieldsPanel {
                            initialSnapshot = response.currentState
                            supportedValues = response.supportedValues
                        }

                        SnapshotJsonPanel {
                            snapshot = response.currentState
                        }
                    }
                }
            }
        }
    }
