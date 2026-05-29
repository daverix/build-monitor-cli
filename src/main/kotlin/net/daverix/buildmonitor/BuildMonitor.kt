package net.daverix.buildmonitor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.util.*

@Serializable
data class JenkinsUser(val id: String, val fullName: String)

@Serializable
data class JenkinsBuild(
    val building: Boolean = false,
    val estimatedDuration: Int? = null,
    val number: Int,
    val timestamp: Long,
    val culprits: List<JenkinsUser> = emptyList()
)

@Serializable
data class JenkinsJob(
    val _class: String,
    val name: String,
    val fullName: String,
    val disabled: Boolean = false,
    val buildable: Boolean = false,
    val lastBuild: JenkinsBuild? = null,
    val lastFailedBuild: JenkinsBuild? = null,
    val lastStableBuild: JenkinsBuild? = null,
    val lastUnstableBuild: JenkinsBuild? = null,
    val jobs: List<JenkinsJob> = emptyList()
)

@Serializable
data class JenkinsJobs(val jobs: List<JenkinsJob>)

suspend fun main(args: Array<String>) {
    var host: String? = null
    var authors: List<String> = emptyList()
    var excludePattern: Regex? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--authors" -> authors = args[++i].split(",").map { it.trim() }
            "--exclude" -> excludePattern = Regex(args[++i])
            else -> host = args[i]
        }
        i++
    }

    requireNotNull(host) { "Usage: build-monitor <jenkins-url> [--authors <name1,name2>] [--exclude <regex>]" }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    val buildParameters = "number,building,timestamp,estimatedDuration,culprits[id,fullName]"
    val jobParameters = "name,fullName,disabled,buildable,lastStableBuild[$buildParameters],lastFailedBuild[$buildParameters],lastUnstableBuild[$buildParameters],lastBuild[$buildParameters]"
    val response: HttpResponse = client.get("$host/api/json?pretty=true&tree=jobs[$jobParameters,jobs[$jobParameters]]")
    val jenkinsJobs: JenkinsJobs = response.body()

    val enabledJobs = jenkinsJobs.jobs
        .filter { !it.disabled }
        .flatMap { job ->
            if(job._class == "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject")
                job.jobs.filter { childJob -> childJob.buildable }
            else if(job.buildable)
                listOf(job)
            else
                emptyList()
        }
        .sortedBy { it.lastBuild?.timestamp }
        .filter { it.matchesAuthors(authors) }
        .filter { job -> excludePattern == null || !excludePattern.containsMatchIn(URLDecoder.decode(job.fullName, "UTF-8")) }

    val failedBuilds = enabledJobs.filter { it.lastBuild != null && it.lastBuild.number == it.lastFailedBuild?.number }
    val unstableBuilds = enabledJobs.filter { it.lastBuild != null && it.lastBuild.number == it.lastUnstableBuild?.number }
    val ongoingBuilds = enabledJobs.filter { it.lastBuild?.building == true }

    println("Unstable builds:")
    println()
    unstableBuilds.forEach { job ->
        printJob(
            job = job,
            color = TextColor.Yellow
        )
    }
    if (unstableBuilds.isEmpty()) {
        println("No unstable builds! ✨")
    }
    println()

    println("Failed builds:")
    println()
    failedBuilds.forEach { job ->
        printJob(
            job = job,
            color = TextColor.Red
        )
    }
    if (failedBuilds.isEmpty()) {
        println("No failed builds! ✨")
    }
    println()

    println("Ongoing builds:")
    println()
    val now = System.currentTimeMillis()
    ongoingBuilds.forEach { job ->
        val progress = job.lastBuild?.let { lastBuild ->
            val buildTime = now - lastBuild.timestamp
            ((buildTime / job.lastBuild.estimatedDuration!!.toFloat()) * 100).toInt()
        }
        print("$progress% ")
        printJob(
            job = job,
            color = TextColor.Blue
        )
    }
    if (ongoingBuilds.isEmpty()) {
        println("No builds in progress")
    }
    println()
}

private fun JenkinsJob.matchesAuthors(authors: List<String>): Boolean {
    if (authors.isEmpty()) return true
    val culprits = lastBuild?.culprits ?: return false
    return authors.any { author ->
        culprits.any { culprit -> culprit.fullName.contains(author, ignoreCase = true) }
    }
}

private fun printJob(job: JenkinsJob, color: TextColor) {
    val lastBuildNumber = job.lastBuild?.number
    val lastBuildDate = job.lastBuild?.timestamp?.let { Date(it) }
    val lastStableDate = job.lastStableBuild?.timestamp?.let { Date(it) }
    val lastStableNumber = job.lastStableBuild?.number
    val culprits = job.lastBuild?.culprits?.takeIf { it.isNotEmpty() }
        ?.joinToString { styled(it.fullName, color = TextColor.MagentaBright) } ?: "unknown"

    println(styled(URLDecoder.decode(job.fullName, "UTF-8"), color = color) + " ${
        lastBuildNumber?.let { styled("#$it", bold = true)}} by $culprits at ${
            lastBuildDate?.let { styled(it.toString(), color = TextColor.Green) }}, last stable: ${
        lastStableNumber?.let { styled("#$it", bold = true, color = TextColor.Blue)}
    } ${lastStableDate?.let { styled(it.toString(), color = TextColor.Green) }}")
}
