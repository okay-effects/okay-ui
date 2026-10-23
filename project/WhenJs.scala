import sbt._
import sbtcrossproject.CrossProject

object WhenJs extends AutoPlugin {
  override def trigger = allRequirements
  object autoImport {
    /** a module's Scala.js settings, only when Scala.js is asked (`-Dplatforms=jvm,js`): without it the module has no
     * JS project, and `jsSettings` alone fails to load the build */
    implicit class WhenJsSettings(p: CrossProject) {
      def whenJs(ss: Def.SettingsDefinition*): CrossProject =
        if (sys.props.get("platforms").exists(_.split(',').map(_.trim.toLowerCase).contains("js")))
          p.configurePlatform(scalajscrossproject.JSPlatform)(_.settings(ss: _*))
        else p
    }
  }
}
