# UI Polish branch template

Branch naming convention

- Feature branches: `ui/{{screen}}/polish-<short-description>`
  - Examples: `ui/steps/polish-charts`, `ui/run/polish-map-styles`

Commit message guidelines

- Use present-tense, short summary: `ui(steps): add live step counter display`
- For multi-file changes, include a short bullet list in the body with files changed.

Pull request checklist

- [ ] Title follows `ui(<screen>): short description` convention
- [ ] Screenshots or short GIFs included for UI changes
- [ ] Accessibility labels added where applicable
- [ ] Unit tests or UI tests added/updated

Branch checklist

- [ ] Create branch from `main` or `develop` depending on workflow
- [ ] Run `./gradlew :app:assembleDebug` locally
- [ ] Run unit tests: `./gradlew :app:testDebugUnitTest`
- [ ] Add a short PR description referencing relevant `plan.md` tasks

