package beyond.plugin

import akka.actor.Actor
import org.mozilla.javascript.commonjs.module.ModuleScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import play.api.mvc.Request

class GamePluginWorker(contextFactory: BeyondContextFactory,
                       global: BeyondGlobal,
                       handler: Function) extends Actor {
  import beyond.plugin.RhinoConversions._
  import beyond.plugin.GamePlugin._

  // FIXME: Pass the module URI once we load scripts from file path.
  val scope = new ModuleScope(global, null, null)

  private def handle[A](request: Request[A]): String = contextFactory.call { cx: Context =>
    val scriptableRequest: Scriptable = cx.newObject(scope, "Request", Array(request))
    val args: Array[AnyRef] = Array(scriptableRequest)
    handler.call(cx, scope, scope, args)
  }.toString

  private def invokeFunction(function: Function, args: Array[AnyRef]) {
    contextFactory.call { cx: Context =>
      function.call(cx, scope, scope, args)
    }
  }

  override def receive: Receive = {
    case Handle(request) =>
      sender ! handle(request)
    case InvokeFunction(function, args) =>
      invokeFunction(function, args)
  }
}

