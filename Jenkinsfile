https://github.com/mauriciojost/jenkinslibs
@Library('jenkinslibs') _
scalaPipeline(
  dockerImage: 'mauriciojost/scala-sbt-ci:java-openjdk_8u171-scala-2.12.10-sbt-1.4.2-img-0.3.4.main4ino',
  dockerArgs: '--cpus=1 --memory=4G',
  timeoutMinutes: 30,
  buildsToKeep: "10",
  sbtOpts: "-Dsbt.rootdir=true",
  email: 'mauriciojostx@gmail.com',
  package: '/var/lib/main4ino/server'
)
