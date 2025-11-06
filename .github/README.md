# 🚀 GitHub Actions Workflows

This directory contains the GitHub Actions workflows for automated building, testing, and releasing of GigHub.

## 📋 Workflow Overview

### 🏗️ CI Workflow (`ci.yml`)
**Triggers**: Push to `main`/`develop`, Pull Requests

**Jobs**:
- 🧪 **Test**: Runs unit tests and uploads test results
- 🔨 **Build**: Builds the plugin JAR with optimization
- ✅ **Validate**: Validates plugin structure and requirements
- 🔒 **Security**: Runs security vulnerability scanning
- 📦 **Dependencies**: Checks for dependency updates

**Features**:
- ✅ Java 21 with Temurin distribution
- 📊 Test coverage reporting
- 📏 Plugin size validation (target: <15MB)
- 🔍 Trivy security scanning
- 📦 Dependency update notifications

### 🚀 Release Workflow (`release.yml`)
**Triggers**: Git tags matching `v*` (e.g., `v1.0.0`, `v1.1.0-alpha`)

**Jobs**:
- 🚀 **Release**: Creates GitHub release with built JAR
- 🧪 **Test Release**: Validates the release JAR
- 📢 **Notify**: Creates release summary and notifications

**Features**:
- 🏷️ Automatic version extraction from git tags
- 📝 Changelog generation from git history
- 📦 Release artifact upload
- 🎯 Pre-release detection for alpha/beta/rc versions
- ✅ Release validation and testing

### 📦 Dependencies Workflow (`dependencies.yml`)
**Triggers**: Weekly schedule (Mondays 9:00 UTC), Manual dispatch

**Jobs**:
- 🔍 **Check Updates**: Scans for dependency updates
- 🔄 **Update Dependencies**: Updates dependencies (manual)

**Features**:
- 📅 Weekly dependency scanning
- 🔄 Manual dependency updates
- 📢 Automatic issue creation for updates
- 📊 Update reports and summaries

### 📚 Documentation Workflow (`docs.yml`)
**Triggers**: Push to `main`, Documentation changes, Manual dispatch

**Jobs**:
- 🔍 **Check Docs**: Validates documentation structure
- 🔤 **Spell Check**: Checks spelling in documentation
- 📚 **Generate Docs**: Creates comprehensive documentation

**Features**:
- 📖 Documentation validation
- 🔤 Spell checking with custom dictionary
- 📚 API documentation generation
- 📊 Documentation statistics

## 🎯 Usage Guide

### 🚀 Creating a Release

1. **Update version in development**:
   ```bash
   # Ensure pom.xml has SNAPSHOT version
   mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
   ```

2. **Commit and merge changes**:
   ```bash
   git add .
   git commit -m "feat: Prepare for v1.1.0 release"
   git push origin develop
   ```

3. **Create and push tag**:
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

4. **Watch the magic** 🎉:
   - GitHub Actions automatically triggers
   - Release is created with built JAR
   - Changelog is generated from commits
   - Release is validated and tested

### 📦 Managing Dependencies

1. **Check for updates** (automatic weekly):
   - GitHub creates issue with available updates
   - Review the update report
   - Decide which updates to apply

2. **Manual dependency update**:
   - Go to Actions → Dependencies workflow
   - Click "Run workflow"
   - Choose update type (minor/major/patch)
   - Changes are automatically committed

### 🔍 Monitoring Builds

1. **CI Status**:
   - Check Actions tab for build status
   - Review test results and coverage
   - Monitor plugin size and security scans

2. **Release Validation**:
   - Each release is automatically tested
   - JAR structure and size validated
   - Release summaries generated

## ⚙️ Configuration

### 🏷️ Version Management

Versions are automatically extracted from git tags:
- `v1.0.0` → `1.0.0`
- `v1.1.0-alpha` → `1.1.0-alpha` (pre-release)
- `v2.0.0-beta.1` → `2.0.0-beta.1` (pre-release)

### 📏 Build Optimization

The CI workflow includes several optimizations:
- `minimizeJar=true` in Maven Shade plugin
- Dependency filtering to reduce JAR size
- Size validation (target: <15MB)
- SQLite only for alpha releases

### 🔒 Security Features

- **Trivy scanning**: Vulnerability detection
- **Dependency checks**: Outdated package detection
- **Artifact validation**: JAR structure verification
- **Secret scanning**: GitHub built-in protection

## 🛠️ Local Development

### 🧪 Running Tests Locally

```bash
# Run all tests
mvn clean test

# Run tests with coverage
mvn clean test jacoco:report

# Check for dependency updates
mvn versions:display-dependency-updates
```

### 🚀 Building Releases Locally

```bash
# Build with specific version
mvn clean package -Drevision=1.0.0 -DskipTests

# Build optimized JAR
mvn clean package -DskipTests -Pproduction
```

### 📚 Validating Plugin

```bash
# Check JAR structure
jar tf target/GigHub-1.0.0.jar | grep plugin.yml

# Check JAR size
ls -lh target/GigHub-1.0.0.jar

# Validate main class
jar tf target/GigHub-1.0.0.jar | grep GigHubPlugin
```

## 🔧 Troubleshooting

### ❌ Common Issues

**Build fails with "No plugin.yml found"**:
- Check that `plugin.yml` is in `src/main/resources`
- Verify resource filtering configuration
- Check Maven build phases

**Release workflow fails on version extraction**:
- Ensure tag follows `v*.*.*` format
- Check that tag is pushed to repository
- Verify git fetch depth in workflow

**Plugin size too large**:
- Check dependency shading configuration
- Verify `minimizeJar=true` is set
- Review included dependencies

**Tests fail in CI but pass locally**:
- Check Java version compatibility
- Verify test resource locations
- Review Maven surefire configuration

### 🔍 Debugging

1. **Check workflow logs**:
   - Go to Actions tab
   - Click on failed workflow run
   - Review job logs and error messages

2. **Debug locally**:
   ```bash
   # Replicate CI environment
   docker run -it --rm -v $(pwd):/workspace -w /workspace openjdk:21-jdk mvn clean test
   
   # Check specific issues
   mvn dependency:tree
   mvn help:effective-pom
   ```

3. **Validate configuration**:
   ```bash
   # Check Maven configuration
   mvn help:effective-pom
   
   # Verify plugin configuration
   mvn help:describe -Dplugin=maven-shade-plugin
   ```

## 📊 Metrics and Monitoring

### 📈 Build Statistics

- **Build Time**: Typically 2-3 minutes
- **Test Coverage**: Target >80%
- **Plugin Size**: Target <15MB
- **Security Score**: Zero critical vulnerabilities

### 📊 Release Metrics

- **Release Time**: ~5 minutes
- **Artifact Validation**: Automated
- **Changelog Quality**: Auto-generated from commits
- **Release Success Rate**: Target 100%

## 🔄 Workflow Evolution

### 🚀 Planned Improvements

- **Multi-platform builds**: Windows, macOS, Linux
- **Integration testing**: Test server deployment
- **Performance testing**: Load testing with mock data
- **Documentation deployment**: Auto-deploy to GitHub Pages
- **Release notifications**: Discord/Slack integration

### 📝 Version History

- **v1.0**: Initial CI/CD setup
- **v1.1**: Added security scanning and dependency management
- **v1.2**: Enhanced documentation workflow
- **v1.3**: Performance optimizations and monitoring

---

## 📞 Support

For workflow issues:
1. Check this README first
2. Review workflow logs
3. Check GitHub Actions documentation
4. Create an issue with detailed information

**Happy automating!** 🎉
