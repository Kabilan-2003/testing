# AI-Powered Jira Integration for Test Automation

An IntelliJ plugin that automatically creates Jira tickets from failed test cases with AI-powered descriptions and root cause analysis.

## 🚀 Features

### Phase 1 (MVP) - Current
- ✅ **Manual API Token Authentication** - Configure Jira URL, Project Key, Email, and API Token
- ✅ **Test Failure Detection** - Monitors Rest Assured and other test frameworks
- ✅ **Approval Workflow** - "Approve Jira Ticket?" popup before creating tickets
- ✅ **Jira Integration** - Creates tickets via REST API with smart descriptions
- ✅ **Settings Panel** - Comprehensive configuration options
- ✅ **Tool Windows** - Dedicated panels for test failures and Jira tickets

### Phase 2 (AI Features) - Planned
- 🤖 **AI-Generated Descriptions** - Smart bug reports with root cause analysis
- 🎯 **Severity Prediction** - Automatic priority assignment
- 🔍 **Duplicate Detection** - Prevents creating duplicate tickets
- 📊 **Test Clustering** - Groups similar failures into single tickets

### Phase 3 (Usability) - Planned
- 📱 **Enhanced UI** - Rich tool windows with filtering and search
- 📈 **Analytics Dashboard** - Pass/fail rates, ticket metrics
- 🔔 **Smart Notifications** - IntelliJ balloon alerts
- 💾 **Data Storage** - SQLite/Postgres integration

### Phase 4 (Enterprise) - Planned
- 🔐 **OAuth 2.0** - "Login with Atlassian" authentication
- 👥 **Team Features** - Multi-user support, role management
- 🔄 **Advanced Workflows** - Custom ticket templates, auto-assignment

## 🛠 Supported Test Frameworks

- **Rest Assured** - API testing framework
- **JUnit 4/5** - Java unit testing
- **TestNG** - Advanced testing framework
- **Spock** - Groovy testing framework

## 📋 Installation

### Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd jira-test-automation-plugin
   ```

2. **Build the plugin**
   ```bash
   ./gradlew buildPlugin
   ```

3. **Run in IntelliJ**
   ```bash
   ./gradlew runIde
   ```

### Production Installation

1. Download the plugin from the IntelliJ Marketplace (coming soon)
2. Install through IntelliJ: `File > Settings > Plugins > Marketplace`
3. Restart IntelliJ

## ⚙️ Configuration

1. **Open Settings**: `File > Settings > Tools > Jira Test Automation`

2. **Configure Jira**:
   - **Jira URL**: Your Jira instance URL (e.g., `https://yourcompany.atlassian.net`)
   - **Project Key**: The project key where tickets will be created
   - **Email**: Your Jira account email
   - **API Token**: Generate from [Atlassian Account Settings](https://id.atlassian.com/manage-profile/security/api-tokens)

3. **Configure Plugin**:
   - **Auto-detect failures**: Enable automatic detection
   - **Show approval dialog**: Confirm before creating tickets
   - **Default Assignee**: Auto-assign tickets
   - **Default Priority**: Set ticket priority

4. **Configure AI** (Phase 2):
   - **Enable AI**: Turn on AI features
   - **AI Provider**: Choose OpenAI or local models
   - **API Key**: Your AI provider API key

## 🎯 Usage

### Automatic Mode
1. Run your tests in IntelliJ
2. When a test fails, the plugin detects it automatically
3. If approval dialog is enabled, confirm ticket creation
4. Ticket is created with detailed description and logs
5. View created tickets in the "Jira Tickets" tab

### Manual Mode
1. Right-click on a failed test
2. Select "Create Jira Ticket" from context menu
3. Or use the main menu: `Run > Create Jira Ticket from Test Failure`

### Viewing Results
- **Test Failures Tab**: Shows all detected test failures
- **Jira Tickets Tab**: Shows created tickets with links
- **Clickable Links**: Click on ticket keys to open in Jira

## 🔧 Development

### Project Structure
```
src/main/kotlin/com/jira/testautomation/
├── models/           # Data models (TestFailure, JiraTicket)
├── services/         # Core services (JiraApiService, AIService)
├── settings/         # Settings and configuration
├── ui/              # UI components (tool windows, panels)
├── actions/         # IntelliJ actions and menus
└── listeners/       # Test execution listeners
```

### Key Components

- **JiraApiService**: Handles Jira REST API communication
- **JiraIntegrationService**: Main orchestration service
- **TestRunListener**: Detects test failures
- **TestFailuresPanel**: UI for displaying failures and tickets
- **JiraSettings**: Persistent configuration storage

### Building and Testing

```bash
# Build plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run in development IDE
./gradlew runIde

# Build for distribution
./gradlew buildPlugin && ./gradlew publishPlugin
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Documentation**: [Wiki](https://github.com/your-repo/wiki)
- **Discussions**: [GitHub Discussions](https://github.com/your-repo/discussions)

## 🗺 Roadmap

- [ ] **Phase 1**: Basic Jira integration (Current)
- [ ] **Phase 2**: AI-powered features
- [ ] **Phase 3**: Enhanced UI and analytics
- [ ] **Phase 4**: Enterprise features and OAuth
- [ ] **Future**: SaaS offering and team collaboration

---

**Made with ❤️ for QA teams everywhere**
