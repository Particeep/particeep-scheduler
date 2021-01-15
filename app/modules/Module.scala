package modules

import com.google.inject.AbstractModule

/**
 * doc : https://www.playframework.com/documentation/2.6.x/ScalaDependencyInjection#Programmatic-bindings
 */
class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[OnStart]).asEagerSingleton
  }
}
