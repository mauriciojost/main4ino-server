https://github.com/mauriciojost/jenkinslibs
@Library('jenkinslibs') _
scalaPipeline(
  dockerImage: 'mauriciojost/scala-sbt-ci:java-openjdk_8u171-scala-2.12.10-sbt-1.3.13-img-0.3.0',
  dockerArgs: '--cpus=1 --memory=4G',
  timeoutMinutes: 15,
  buildsToKeep: 10,
  email: 'mauriciojostx@gmail.com'
)
