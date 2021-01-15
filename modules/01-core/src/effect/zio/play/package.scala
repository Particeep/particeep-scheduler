package effect.zio

import effect.zio.sorus.ZioSorus

package object play {

  trait ZioController[Config] extends ZioSorus with ZioAction[Config] with ZioPlayHelper
}
