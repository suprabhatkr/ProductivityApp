---
name: UI polish
about: Issue template for UI polish/visual improvements (per-screen)
title: "UI: {{screen}} — polish"
labels: ui, polish
assignees: ''

---

Context

Provide a short description of the visual/UX polish requested, the target screen/component and any mockups or references.

Steps / Checklist

- [ ] Confirm current `Screen` file and ViewModel: `app/src/main/java/.../ui/{{screen}}/{{ScreenFile}}.kt`
- [ ] Update theming and per-feature accent in `app/src/main/java/.../app/ui/theme/Theme.kt` as needed
- [ ] Add or update accessibility labels and content descriptions for interactive elements
- [ ] Implement polish items (icons, spacing, charts, animations) and reference the files changed
- [ ] Add a Compose UI test or screenshot test for the most important interaction
- [ ] Link to a PR that implements the change

Notes

- When possible, include small screenshots or a short GIF demonstrating before/after.
- If the change impacts navigation or persisted state, describe required state migration or backward compatibility.

