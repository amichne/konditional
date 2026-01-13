package demo

import io.amichne.konditional.uiktor.demo.installDemoKonditionalReactUi
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::demoModule)
        .start(wait = true)
}

fun Application.demoModule() {
    routing {
//        installDemoKonditionalUi()
        installDemoKonditionalReactUi()
    }
}
