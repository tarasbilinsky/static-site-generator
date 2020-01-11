import com.softwaremill.macwire.wire
import controllers.AssetsComponents
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator}
import router.Routes

class AppLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new AppComponents(context).application
  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with AssetsComponents {

  private val prefix: String = "/"
  private val staticRoute: myRouter.Static = wire[myRouter.Static]
  override val router: Router = wire[Routes]
  override val httpFilters: Seq[EssentialFilter] = Seq()
}


