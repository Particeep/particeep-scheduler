name := """particeep-scheduler"""

lazy val commonSettings = Seq(
  organization := "com.particeep",
  version := "1.0.0",
  scalaVersion := "2.13.4",
//  resolvers ++= Seq(
//    "bitbucket-release" at "https://bitbucket.org/Adrien/particeep-repository/raw/master/repository/"
//    , "Bintray_DL" at "https://dl.bintray.com/kamon-io/releases/"
//    , "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
//    , "Kaliber Internal Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"
//    , Resolver.jcenterRepo
//    , Resolver.mavenCentral
//  ),
  libraryDependencies ++= (deps_common ++ deps_db ++ deps_ux ++ deps_crypto ++ deps_tests),
  // don't run test in parallel. It will break the DB
  // concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  scalacOptions ++= compiler_option,
//  routesGenerator := InjectedRoutesGenerator,
  updateOptions := updateOptions.value.withCachedResolution(true),
  sources in (Compile, doc) := Seq.empty
)

lazy val playSettings = commonSettings ++ Seq(
  TwirlKeys.templateImports ++= Seq("domain._")
)

lazy val core: Project = (project in file("modules/01-core"))
  .settings(commonSettings: _*)

lazy val domain: Project     = (project in file("modules/02-domain"))
  .settings(commonSettings: _*)
  .dependsOn(core % "test->test;compile->compile")

lazy val repository: Project = (project in file("modules/03-repository"))
  .settings(commonSettings: _*)
  .dependsOn(domain)
  .dependsOn(core % "test->test;compile->compile")

lazy val services: Project   = (project in file("modules/04-services"))
  .settings(commonSettings: _*)
  .dependsOn(domain)
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(repository % "test->test;compile->compile")

lazy val api: Project        = (project in file("modules/05-api"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .dependsOn(domain)
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(repository % "test->test;compile->compile")
  .dependsOn(services % "test->test;compile->compile")

lazy val web: Project        = (project in file("modules/06-web"))
  .enablePlugins(PlayScala)
  .settings(playSettings: _*)
  .settings(aggregateReverseRoutes := Seq(api))
  .dependsOn(domain)
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(repository % "test->test;compile->compile")
  .dependsOn(services % "test->test;compile->compile")

lazy val root: Project       = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(playSettings: _*)
  .aggregate(core, domain, repository, services, api, web)
  .dependsOn(core % "test->test;compile->compile", domain, repository, services, api, web)

val zio_version              = "1.0.3"
lazy val deps_common         = Seq(
  guice,
  filters,
  ehcache,
  ws,
  "com.typesafe.play" %% "play-json"                   % "2.8.1" withSources (),
  "com.papertrailapp"  % "logback-syslog4j"            % "1.0.0" withSources (),
  "pl.iterators"      %% "kebs-tagged"                 % "1.8.1" withSources (),
  "pl.iterators"      %% "kebs-slick"                  % "1.8.1" withSources (),
  "pl.iterators"      %% "kebs-play-json"              % "1.8.1" withSources (),
  "dev.zio"           %% "zio"                         % zio_version withSources (),
  "dev.zio"           %% "zio-streams"                 % zio_version withSources (),
  "dev.zio"           %% "zio-interop-reactivestreams" % "1.0.3.5-RC12" withSources (),
  "ai.x"              %% "play-json-extensions"        % "0.42.0" withSources ()
)

lazy val deps_crypto = Seq(
  "commons-codec" % "commons-codec" % "1.13" withSources (),
  "org.mindrot"   % "jbcrypt"       % "0.4" withSources ()
)

lazy val deps_ux = Seq(
  "com.nappin" %% "play-recaptcha" % "2.4" withSources () excludeAll (ExclusionRule(organization =
    "com.typesafe.play")),
  "com.ibm.icu" % "icu4j"          % "65.1" withSources ()
)

lazy val deps_tests = Seq(
  "org.scalatestplus.play"   %% "scalatestplus-play" % "5.0.0" % Test withSources (),
  "de.leanovate.play-mockws" %% "play-mockws"        % "2.8.0" % Test withSources ()
)

val play_slick_version = "5.0.0"
val slick_pg_version   = "0.18.1"

lazy val deps_db = Seq(
  "org.postgresql"       % "postgresql"            % "42.2.6" withSources (),
  "com.typesafe.play"   %% "play-slick"            % play_slick_version withSources (),
  "com.typesafe.play"   %% "play-slick-evolutions" % play_slick_version withSources (),
  "com.github.tminglei" %% "slick-pg"              % slick_pg_version withSources (),
  "com.github.tminglei" %% "slick-pg_play-json"    % slick_pg_version withSources ()
)

addCommandAlias("fmt", "; scalafix RemoveUnused; scalafix SortImports; all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "; scalafixAll --check; all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val compiler_option = Seq(
  "-deprecation",                  // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8",                         // Specify character encoding used by source files.
  "-explaintypes",                 // Explain type errors in more detail.
  "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
  "-language:postfixOps",
  "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds",         // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
  // "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",               // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
  "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",        // Option.apply used implicit view.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",              // Warn when dead code is identified.
  "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen",          // Warn when numerics are widened.
  // "-Ywarn-unused:implicits",      // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",         // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",          // Warn if a local definition is unused.
  //"-Ywarn-unused:params",          // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
  //"-Ywarn-unused:privates",        // Warn if a private member is unused.
  "-Ywarn-value-discard"           // Warn when non-Unit expression results are unused.
)

inThisBuild(
  List(
    semanticdbEnabled := true,                        // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    scalafmtOnCompile := true
  )
)

scalafixDependencies in ThisBuild ++= Seq(
  "com.nequissimus" %% "sort-imports" % "0.5.5"
)
