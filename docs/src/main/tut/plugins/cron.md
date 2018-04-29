---
layout: docs
title:  "Cron jobs"
---

# Cron jobs

Orchestra supports Cron jobs via the `orchestra-cron` library/plugin.
```scala
libraryDependencies += "com.drivetribe" %%% "orchestra-cron" % orchestraVersion
```

Orchestra Cron jobs have a one to one mapping to [Kubernetes CronJobs](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/).

To add cron support, we mix in the trait `CronTriggers`, which requires us to implement
`cronTriggers: Set[CronTrigger]`.

Let's write a job that is triggered every 5min:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
// We import the Cron package
import com.drivetribe.orchestra.cron._
import com.drivetribe.orchestra.job._
import com.drivetribe.orchestra.model._

object Orchestration extends Orchestra with CronTriggers { // Note that we mix in GithubHooks
  lazy val board = Folder("Orchestra")(cronJobBoard)
  lazy val jobs = Set(cronJob) // We still need to add the Job to jobs

  // We add the CronTrigger to the cronTriggers
  lazy val cronTriggers = Set(CronTrigger("*/5 * * * *", cronJob))

  lazy val cronJobBoard = JobBoard[() => Unit](JobId("cron"), "Cron")()
  lazy val cronJob = Job(cronJobBoard) { implicit workDir => () =>
    println("Hey!")
  }
}
```