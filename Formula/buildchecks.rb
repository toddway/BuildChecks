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
  url "https://github.com/toddway/BuildChecks/releases/download/v4.0.4/buildchecks-4.0.4-all.jar", using: :nounzip
  sha256 "66a4b72048386a74c151a9ab625c856d8b01cdcabc2d6b177e53c804798598cc"
  license "Apache-2.0"

  depends_on "openjdk"

  def install
    libexec.install "buildchecks-4.0.4-all.jar"
    # write_jar_script writes a bin/ wrapper that runs `java -jar <jar>` with the right JDK.
    bin.write_jar_script libexec/"buildchecks-4.0.4-all.jar", "buildchecks"
  end

  test do
    assert_match "check", shell_output("#{bin}/buildchecks --help")
  end
end
