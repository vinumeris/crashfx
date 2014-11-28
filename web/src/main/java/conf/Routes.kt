package conf

import com.google.inject.Inject
import ninja.Router
import ninja.application.ApplicationRoutes
import ninja.utils.NinjaProperties
import controllers.CrashController
import controllers.DashboardController
import ninja.AssetsController

public class Routes : ApplicationRoutes {
    override fun init(router: Router) {
        router.POST().route("/crashfx/upload").with(javaClass<CrashController>(), "crashUpload")
        router.GET().route("/crashfx/dashboard").with(javaClass<DashboardController>(), "render")

        val assets = javaClass<AssetsController>()
        router.GET().route("/{fileName: .*}").with(assets, "serveStatic")
    }
}
