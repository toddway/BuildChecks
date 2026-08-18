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
  url "https://github.com/toddway/BuildChecks/releases/download/v4.1.2/buildchecks-4.1.2-all.jar", using: :nounzip
  sha256 "44ab0b86a111cf6de037b48d04cec84c20eadb3a5a36eb5ec226b51631f8276b"
  license "Apache-2.0"

  depends_on "openjdk"

  def install
    libexec.install "buildchecks-4.1.2-all.jar"
    # write_jar_script writes a bin/ wrapper that runs `java -jar <jar>` with the right JDK.
    bin.write_jar_script libexec/"buildchecks-4.1.2-all.jar", "buildchecks"
  end

  test do
    assert_match "check", shell_output("#{bin}/buildchecks --help")
  end
end
