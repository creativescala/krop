/*
 * Copyright 2015-2020 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.sys.process.*
import creativescala.ExternalLink
import laika.config.LinkConfig
import laika.config.ApiLinks
import laika.theme.Theme
import laika.helium.config.TextLink

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / tlBaseVersion := "0.10" // your current series x.y

ThisBuild / organization := "org.creativescala"
ThisBuild / organizationName := "Creative Scala"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("noelwelsh", "Noel Welsh")
)

ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeLegacy

lazy val scala3 = "3.6.4"

ThisBuild / crossScalaVersions := List(scala3)
ThisBuild / githubWorkflowJavaVersions := List(JavaSpec.temurin("11"))
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / useSuperShell := true
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / tlSitePublishBranch := Some("main")

// Run this (build) to do everything involved in building the project
commands += Command.command("build") { state =>
  "clean" ::
    "compile" ::
    "test" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    "scalafmtSbt" ::
    "headerCreateAll" ::
    "githubWorkflowGenerate" ::
    "dependencyUpdates" ::
    "reload plugins; dependencyUpdates; reload return" ::
    "docs / tlSite" ::
    state
}

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    Dependencies.munitCats.value,
    Dependencies.log4cats.value,
    Dependencies.http4sClient.value,
    Dependencies.http4sServer.value,
    Dependencies.http4sDsl.value,
    Dependencies.http4sCirce.value,
    Dependencies.scalaTags.value,
    Dependencies.twirl.value
  )
)

lazy val krop = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(moduleName := "krop")

lazy val kropJs =
  krop.js.aggregate(core.js)

lazy val rootJvm =
  krop.jvm.aggregate(
    core.jvm,
    sqlite,
    examples,
    unidocs
  )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(
    commonSettings,
    moduleName := "krop-core"
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      Dependencies.declineEffect.value,
      Dependencies.log4catsSlf4j.value
    )
  )

lazy val sqlite = project
  .in(file("sqlite"))
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Dependencies.sqlite.value,
      Dependencies.magnum.value
    ),
    moduleName := "krop-sqlite"
  )

lazy val docs =
  project
    .in(file("docs"))
    .settings(
      tlSiteApiUrl := Some(
        sbt.url(
          "https://javadoc.io/doc/org.creativescala/krop-docs_3/latest/"
        )
      ),
      laikaConfig := laikaConfig.value.withConfigValue(
        LinkConfig.empty
          .addApiLinks(
            ApiLinks(baseUri =
              "https://javadoc.io/doc/org.creativescala/krop-docs_3/latest/"
            )
          )
      ),
      mdocIn := file("docs/src/pages"),
      mdocVariables := {
        mdocVariables.value ++ Map()
      },
      Laika / sourceDirectories ++=
        Seq(
          // (examples.js / Compile / fastOptJS / artifactPath).value
          //   .getParentFile() / s"${(examples.js / moduleName).value}-fastopt"
        ),
      laikaTheme := CreativeScalaTheme.empty
        .withHome(
          TextLink.internal(laika.ast.Path.Root / "README.md", "Krop")
        )
        .withCommunity(
          ExternalLink("https://discord.gg/rRhcFbJxVG", "Community")
        )
        .withApi(
          ExternalLink(
            "https://javadoc.io/doc/org.creativescala/krop-docs_3/latest",
            "API"
          )
        )
        .withSource(
          ExternalLink(
            "https://github.com/creativescala/krop",
            "Source"
          )
        )
        .build,
      laikaExtensions ++= Seq(
        laika.format.Markdown.GitHubFlavor,
        laika.config.SyntaxHighlighting
      ),
      tlSite := Def
        .sequential(
          // (examples.js / Compile / fastLinkJS),
          mdoc.toTask(""),
          laikaSite
        )
        .value,
      libraryDependencies += Dependencies.circeGeneric.value
    )
    .enablePlugins(TypelevelSitePlugin)
    .dependsOn(core.jvm, sqlite)

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin) // also enables the ScalaUnidocPlugin
  .settings(
    name := "krop-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inAnyProject -- inProjects(
        docs
      )
  )

// To avoid including this in the core build
lazy val examples = project
  .in(file("examples"))
  .settings(
    commonSettings,
    moduleName := "krop-examples",
    mimaPreviousArtifacts := Set.empty,
    // This sets Krop into development mode, which gives useful tools for
    // developers. If you don't set this, Krop runs in production mode.
    run / javaOptions += "-Dkrop.mode=development",
    run / fork := true,
    libraryDependencies += Dependencies.logback.value
  )
  .dependsOn(core.jvm)
