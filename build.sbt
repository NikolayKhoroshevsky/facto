name := """facto"""
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
javaOptions in Test += "-Dconfig.file=conf/testing/application.conf"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  
  "org.yaml"               %  "snakeyaml"             % "1.14",
  "com.github.nscala-time" %% "nscala-time"           % "2.12.0",
  "com.typesafe.slick"     %% "slick"                 % "3.0.0",
  "commons-lang"           %  "commons-lang"          % "2.6",
  "mysql"                  %  "mysql-connector-java"  % "5.1.36",
  "org.xerial"             %  "sqlite-jdbc"           % "3.8.11.2",


  "org.webjars"            %% "webjars-play"          % "2.4.0-1",
  "org.webjars"            %  "bootstrap"             % "3.3.1",
  "org.webjars"            %  "datatables"            % "1.10.4",
  "org.webjars"            %  "datatables-plugins"    % "1.10.7",
  "org.webjars"            %  "flot"                  % "0.8.3",
  "org.webjars"            %  "font-awesome"          % "4.2.0",
  "org.webjars.bower"      %  "holderjs"              % "2.6.0",
  "org.webjars"            %  "metisMenu"             % "1.1.3",
  "org.webjars"            %  "morrisjs"              % "0.5.1",
  "org.webjars.bower"      %  "datatables-responsive" % "1.0.6",
  "org.webjars"            %  "bootstrap-social"      % "4.9.0",
  "org.webjars.bower"      %  "flot.tooltip"          % "0.8.5"
)
