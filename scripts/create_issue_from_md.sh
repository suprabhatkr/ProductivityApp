#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/create_issue_from_md.sh <path-to-markdown-issue>
# Example: ./scripts/create_issue_from_md.sh .github/ISSUES/2026-04-11-enable-paparazzi-polish-tests.md

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <path-to-markdown-issue>" >&2
  exit 2
fi

ISSUE_MD="$1"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ABS_PATH="$REPO_ROOT/$ISSUE_MD"

if [ ! -f "$ABS_PATH" ]; then
  echo "Issue file not found: $ABS_PATH" >&2
  exit 3
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "Error: gh (GitHub CLI) is not installed or not in PATH. Install from https://cli.github.com/" >&2
  exit 4
fi

# Extract title from YAML frontmatter (title: "...")
TITLE_LINE=$(sed -n '1,40p' "$ABS_PATH" | grep -m1 '^title:') || true
if [ -z "$TITLE_LINE" ]; then
  echo "Warning: title not found in frontmatter; using filename as title." >&2
  TITLE="$(basename "$ISSUE_MD")"
else
  # Remove leading 'title:' and surrounding quotes
  TITLE=$(printf "%s" "$TITLE_LINE" | sed -E 's/^title:[[:space:]]*"?(.*)"?$/\1/' | sed 's/^\s*"\?//;s/"\?\s*$//')
fi

# Create a temporary body file that removes YAML frontmatter if present
# Create a portable temporary file for the issue body. Use mktemp -t on macOS
if [ "$(uname)" = "Darwin" ]; then
  # On macOS, prefer `mktemp -t prefix` which creates a temp file in the OS
  # temp directory even if TMPDIR isn't usable. Append .md for clarity.
  if BODYFILE=$(mktemp -t create_issue_body 2>/dev/null); then
    mv "$BODYFILE" "${BODYFILE}.md" || true
    BODYFILE="${BODYFILE}.md"
    # ensure file exists
    : > "$BODYFILE"
  else
    # fallback to /tmp
    BODYFILE=$(mktemp /tmp/create_issue_body.XXXXXX.md)
  fi
else
  BODYFILE=$(mktemp --tmpdir create_issue_body.XXXXXX.md)
fi

# If file starts with '---', strip until the second '---'
if sed -n '1p' "$ABS_PATH" | grep -q '^---$'; then
  # drop frontmatter
  awk 'BEGIN{in=0} /^---$/{if(in==0){in=1;next} else {in=2; next}} in==0{print} in==2{print}' "$ABS_PATH" | sed '1,2d' > "$BODYFILE" || true
  # The above awk/sed combo aims to remove the initial frontmatter block. If it fails, fall back to using whole file.
  if [ ! -s "$BODYFILE" ]; then
    # fallback: use entire file
    cp "$ABS_PATH" "$BODYFILE"
  fi
else
  cp "$ABS_PATH" "$BODYFILE"
fi

# Optionally add labels and assignees here
LABELS=(testing ci paparazzi)
LABEL_ARGS=()
for l in "${LABELS[@]}"; do
  LABEL_ARGS+=(--label "$l")
done

# Ensure labels exist in the target repository. If a label does not exist,
# try to create it (best-effort). This avoids gh failing when adding unknown labels.
for l in "${LABELS[@]}"; do
  if ! gh label view --json name "$l" >/dev/null 2>&1; then
    echo "Label '$l' not found in repo; attempting to create it (best-effort)"
    # Best-effort creation; choose a neutral color. If creation fails (permissions), continue.
    if ! gh label create "$l" --color f29513 --description "auto-created label: $l" >/dev/null 2>&1; then
      echo "Warning: failed to create label '$l'. Issue creation will continue without creating this label." >&2
    fi
  fi
done

# Create the issue (the script assumes 'gh' is authenticated)
# If you need to target a specific repo, add --repo owner/repo to the gh command below.
set -x
gh issue create --title "$TITLE" --body-file "$BODYFILE" "${LABEL_ARGS[@]}"
set +x

# Cleanup
rm -f "$BODYFILE"

echo "Issue creation command completed. If gh printed a URL, the issue was created."




