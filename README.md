# build-monitor-cli

A command-line tool that connects to a Jenkins server and displays a colour-coded summary of build status:

- 🟡 **Unstable** builds
- 🔴 **Failed** builds
- 🔵 **Ongoing** builds — with percentage progress based on estimated duration

## Requirements

- JDK 11 or newer
- Access to a Jenkins instance

## Running

### With Gradle (development)

```bash
./gradlew run --args="<jenkins-url>"
```

Example:

```bash
./gradlew run --args="https://ci.example.com"
```

### With the distribution (production)

Build a distributable archive:

```bash
./gradlew installDist
```

Then run the generated script:

```bash
./build/install/build-monitor/bin/build-monitor <jenkins-url>
```

Or build a zip/tar archive for deployment:

```bash
./gradlew distZip   # → build/distributions/build-monitor-1.0-SNAPSHOT.zip
./gradlew distTar   # → build/distributions/build-monitor-1.0-SNAPSHOT.tar
```

## Building

```bash
./gradlew build
```

## Output

Each build line shows the job name, build number, culprit(s), timestamp, and last stable build info. Colour rendering requires a terminal that supports ANSI escape codes.
