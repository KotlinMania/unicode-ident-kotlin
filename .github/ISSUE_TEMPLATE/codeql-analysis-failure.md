---
name: CodeQL Analysis Failure
about: Track and resolve issues with CodeQL analysis failure
---

#### Summary:
The CodeQL job in the GitHub Actions workflow failed due to an issue with the `java-kotlin` configuration. In `none` build mode, CodeQL is unable to resolve dependencies correctly during analysis.

#### Logs:
```
Variant Selection Exception: org.gradle.internal.component.resolution.failure.exception.ArtifactSelectionException caused by Resolution Failure
...
CodeQL detected code written in Java/Kotlin but could not process it using the 'none' build mode.
```

#### Suggested Fix:
Update the workflow's Java/Kotlin configuration from `build-mode: none` to `build-mode: manual`. Include a Gradle build step for dependency resolution:

```yaml
- name: Build with Gradle (only applies to java-kotlin)
  if: matrix.language == 'java-kotlin'
  run: ./gradlew build --no-daemon --stacktrace
```