# Homebrew formula for BuildChecks, kept in-repo so the release workflow can bump it with the
# built-in GITHUB_TOKEN (a separate homebrew-tap repo would need an expiring PAT). The `url` and
# `sha256` below are rewritten automatically on each tagged release by .github/workflows/release.yml.
#
# Install:
#   brew tap toddway/buildchecks https://github.com/toddway/BuildChecks
#   brew install buildchecks
class Buildchecks < Formula
  desc "Toolchain-agnostic CLI that aggregates code-analysis and test/coverage reports into one gated summary"
  homepage "https://github.com/toddway/BuildChecks"
  # `using: :nounzip` keeps Homebrew from unpacking the jar (a jar is a zip file).
  url "https://github.com/toddway/BuildChecks/releases/download/v4.0.12/buildchecks-4.0.12-all.jar", using: :nounzip
  sha256 "d188e3ab24851d173c764f1d3e2cb7a4d0f6468cac30305f5b0bc0fd2f67c908"
  license "Apache-2.0"

  depends_on "openjdk"

  def install
    libexec.install "buildchecks-4.0.12-all.jar"
    # write_jar_script writes a bin/ wrapper that runs `java -jar <jar>` with the right JDK.
    bin.write_jar_script libexec/"buildchecks-4.0.12-all.jar", "buildchecks"
  end

  test do
    assert_match "check", shell_output("#{bin}/buildchecks --help")
  end
end
