package io.amichne.konditional.uiktor

import io.amichne.konditional.uiktor.demo.installDemoKonditionalReactUi
import io.amichne.konditional.uiktor.demo.installDemoKonditionalUi
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.module() {
    routing {
        installDemoKonditionalUi()
        installDemoKonditionalReactUi()
    }
}


//Then open:
//
//- GET /config for the full page
//- GET /config/node/{id} for a single node
//- PATCH /config/state for JSON Patch updates
//
//Custom spec/state
//
//import io.amichne.konditional.uiktor.*
//import io.ktor.server.application.*
//import io.ktor.server.routing.*

//fun Application.module() {
//    routing {
//        installUiRoutes(
//            UiRouteConfig(
//                service = ,
//
//                renderer = defaultRenderer(),
//                paths = UiRoutePaths(page = "/config", node = "/config/node/{id}", patch = "/config/state"),
//            )
//        )
//    }
//}
