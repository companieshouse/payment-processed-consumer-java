## Purpose and scope

Use this guide to assess and resolve dependency vulnerabilities while applying
engineering judgement to the service's risk and compatibility needs. It gives
shared guidance for Java and Go and cross-cutting triage advice; authoritative
language- and tool-specific processes take precedence:

* For Node.js, follow the [How to Resolve NPM Dependency Vulnerabilities
  guide](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/6294634516/How+to+Resolve+NPM+Dependency+Vulnerabilities)
  for scanning, resolution strategies and verification.
* For Dependency-Check suppressions, follow the [CH vulnerable dependency
  suppression process](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/4726063220/Vulnerable+dependency+suppression+process).
* For Go, check the repository's supported security check and pipeline.
* For Perl, involve a service maintainer or Perl subject-matter expert.

## Prerequisites

* For Java checks, install `depcheck` and verify it with
  `depcheck --version`.
* For Node.js, follow the tool requirements in the NPM vulnerability guide;
  `depcheck` is only needed if running a Dependency-Check scan.
* Use `docker-chs-development` when local testing requires it.

## Finding the vulnerable dependencies to resolve

1. Understand the full dependency tree of a project

    * For Java run `mvn dependency:tree` in the root of the project to find out
      which dependencies are used and their versions.
    * For Node.js, use `npm audit` and `npm ls` as described in the
      [How to Resolve NPM Dependency Vulnerabilities guide](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/6294634516/How+to+Resolve+NPM+Dependency+Vulnerabilities).
    * For Go, check the repository's Makefile and pipeline configuration for
      the supported dependency security check. For example, some repositories
      provide `make security-check` (also named `make dependency-check`), which
      runs Nancy and `govulncheck`. Do not assume every Go repository has the
      same target or that its pipeline runs it.
   
   It may be useful to store this in a file for later comparison, e.g.
   `mvn dependency:tree > dependency-tree.txt` or
   `npm ls > dependency-tree.txt`.

2. For Java, run `depcheck` in the root of the project to produce a
   vulnerability report. For Node.js, follow the NPM guide's audit and
   diagnostic steps; also review a `depcheck` report if that check is used.
   For Go, run the repository's supported security-check target if one is
   available; do not assume `depcheck` supports Go.
3. Locate the report:

    * For Java this will be
      `./target/dependency-check-report.html`
    * Node.js findings are reported by `npm audit`; if `depcheck` is also run,
      its report will be at
      `./coverage/dependency-check-reports/dependency-check-report.html`.
    * Go checks such as Nancy and `govulncheck` report findings in their command
      output; check the repository's instructions for any additional output.
4. On the report, assess each new vulnerability (if there is any)

   1. Decide if it is a:

       * Genuine Vulnerability - it affects the component as relates to a
         dependency used in the project.
       * False Positive - it does not affect the component as relates to a
         dependency used in the project.

      Support a false-positive or not-applicable assessment with evidence from
      the advisory and the service's code or configuration. Do not assume a
      vulnerability is irrelevant just because the affected feature is not
      believed to be used; proving reachability or non-applicability can be
      difficult. Consult the relevant maintainer or security specialist if
      uncertain.
   2. Assess the risk and the recommended resolution or mitigation. Severity is
      a useful prioritisation input, not a complete risk assessment. Consider
      the affected functionality, exploit prerequisites and exposure, known
      exploitation, available mitigations, and the compatibility and risk of an
      upgrade. Use engineering judgement; these considerations are not an
      exhaustive scoring checklist.
   3. Determine if this vulnerability is likely to affect other applications.
   4. Understand the source of the dependency and where its version is being
      resolved
   5. If the finding is assessed as false positive or not applicable, record
      the evidence and use the relevant tool's supported suppression process
      if the finding continues to be reported. Do not suppress a finding merely
      because its applicability is uncertain.
5. Review all the suppressed vulnerabilities and check

   * Whether the suppression is for a genuine vulnerability or a false positive.
   * Whether the suppression is still valid or if it can be removed.
   * Whether any dependencies need to be upgraded to a newer version that has the vulnerability fixed.
6. View the number of vulnerabilities reported on Dependency Track by checking
   the [services dashboard](https://dev-tools.cidev.aws.chdev.org/dashboard/).
7. If there are critical or high vulnerabilities, check
   [Dependency Track](https://dependency-track.companieshouse.gov.uk/) for more
   information on the vulnerability and the recommended resolution.
   Go projects may submit a CycloneDX SBOM to Dependency Track during release;
   this provides an inventory but may not be a build or PR gate. Check the
   repository's pipeline and confirm the component's status in Dependency Track
   rather than assuming it has been checked or blocked automatically.

## Perl upgrades

Do not attempt Perl vulnerability upgrades independently. Involve the service's
Perl maintainer or another Perl subject-matter expert to assess, implement and
validate the change.

## Resolving new vulnerabilities

**Note:** For Java and Go, prefer resolving a vulnerability through the most direct dependency that can safely provide the fix. For Node.js, follow the ordered strategies in the [How to Resolve NPM Dependency Vulnerabilities guide](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/6294634516/How+to+Resolve+NPM+Dependency+Vulnerabilities), which takes precedence over this general advice.

**Important:** If the dependency version is defined in the Companies House BOM or Shared Library, upgrade the dependency there and then upgrade the project to use the new BOM or Shared Library version.

### If the vulnerability is a genuine vulnerability

You need to tailor your approach based on the severity of the vulnerability.
Prioritise fixing critical and high vulnerabilities first, then medium and low
vulnerabilities, while considering the actual exposure and risk to the service.
For Node.js, the [How to Resolve NPM Dependency Vulnerabilities
guide](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/6294634516/How+to+Resolve+NPM+Dependency+Vulnerabilities)
is authoritative for the order of resolution strategies, npm-specific commands
and verification. Where its instructions differ from the general steps below,
follow the NPM guide.

#### Critical or High Vulnerabilities

**If there is a fixed version or mitigation available:**

1. Identify the dependency to upgrade:

   1. If the vulnerability is in a direct dependency, this will be the
      dependency to upgrade.
   2. If the vulnerability is in a transitive dependency, try and look for
      a newer version of the direct dependency that uses a fixed version of the
      transitive dependency. If there is no fixed version of the direct
      dependency, you may need to upgrade the transitive dependency directly.
2. Decide Where to upgrade the dependency

   1. If the dependency is a direct dependency or transitive dependency of a dependency defined directly in the project and not used elsewhere, upgrade the dependency in the project.
   2. If the dependency is a direct dependency or transitive dependency of a dependency defined in the Companies House BOM or Shared Library, upgrade the dependency in the BOM or Shared Library and then upgrade the dependency in the project to use the new version of the BOM or Shared Library.

3. Make any necessary code changes arising from the version upgrade.
4. Ensure all local tests pass and the build passes.
5. Try to test the application locally in your local docker environment to ensure the application is working as expected.
6. For Go repositories with a security-check target, run it before and after
   the upgrade. Check the resulting dependency and vulnerability status in
   Dependency Track after the release SBOM has been submitted.

**If there isn't a fixed version:**

1. If there are any mitigations or workarounds available, implement them if appropriate. Ensure you have tested and verified your application is working as expected after implementing the mitigation or workaround.
2. If there are not any mitigations or workarounds available, use the
   time-bound suppression or exception process supported by the relevant tool.
   For Java and Node.js findings from Dependency-Check, add a suppression in the
   [dependency-check-suppressions repository](https://github.com/companieshouse/dependency-check-suppressions)
   and raise a PR with a justification and removal plan. Follow the [CH
   vulnerable dependency suppression
   process](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/4726063220/Vulnerable+dependency+suppression+process),
   including updating the shared SEC CVE ticket, creating a subtask for each
   affected repository, and setting the ticket due date to the review date.
   For an `npm audit` finding, confirm the applicable exception process with the
   service or AppSec owner; do not assume Dependency-Check suppressions apply.
   For Go findings from tools such as Nancy or `govulncheck`, confirm the
   supported suppression process with the tool or service maintainer.

### Medium or Low Vulnerabilities

You should use your judgement to decide whether to fix the vulnerability or
suppress it. If you decide to fix it, follow the same steps as for critical or
high vulnerabilities. For Java and Node.js, use the time-bound suppression
process in the [dependency-check-suppressions repository](https://github.com/companieshouse/dependency-check-suppressions) for Dependency-Check findings, following the [CH vulnerable dependency suppression process](https://companieshouse.atlassian.net/wiki/spaces/DEV/pages/4726063220/Vulnerable+dependency+suppression+process). For `npm audit` findings, confirm the applicable exception process with the service or AppSec owner. For Go, confirm the supported suppression process with the tool or service maintainer.

## Resolving suppressed vulnerabilities

If there are any suppressed vulnerabilities, you should review them to determine whether the suppression is still valid or if it can be removed. If the suppression is no longer valid, you should remove the suppression and follow the steps above to resolve the vulnerability.

Follow the same steps as for new vulnerabilities to resolve the suppressed
vulnerabilities. For Java and Node.js, remove the suppression from the
[dependency-check-suppressions repository](https://github.com/companieshouse/dependency-check-suppressions)
when the finding was suppressed in Dependency-Check. For `npm audit` and other
tools, remove or update the exception using that tool's supported process.

If there was a suppression, consider adding or amending the Companies House BOM library or Shared Library to use a fixed version of the dependency if one is available.

## Definitions

* **Direct Dependency** - a dependency that is explicitly declared in the
  project.
* **Transitive Dependency** - a dependency that is not explicitly declared in
  the project, but is pulled in by a direct dependency. Either a direct dependency or another transitive dependency can pull in a transitive dependency.
* **Dependency Tree** - a tree structure that shows the relationships between
  dependencies in a project. It shows which dependencies are direct and which are transitive, as well as the versions of each dependency.
* **Dependency Track** - a tool that provides a Software Bill of Materials (SBOM) and vulnerability management for software projects. It helps identify and track vulnerabilities in dependencies, providing information on their severity and recommended resolutions.
* **Depcheck** - a tool that wraps the dependency-check tools for Java and Node.js, providing a report of vulnerabilities in a project and their severity.
* **Companies House BOM** - a library which defines common dependencies and their versions for use across multiple projects. It helps ensure consistency and compatibility between projects by providing a standard set of dependencies. For Java this is: [ch-boms](https://github.com/companieshouse/ch-boms)
* **BOM (Bill of Materials)** - a file that lists the dependencies of a project, along with their versions and other metadata. It is used to manage dependencies and ensure that the correct versions are used in a project. 
