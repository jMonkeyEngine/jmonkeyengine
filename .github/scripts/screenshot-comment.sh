#!/usr/bin/env bash
# Posts, updates or retires the pull request comment that lists changed screenshots. Run by
# screenshot-comment.yml once a screenshot workflow finishes.
#
# The results it reads were uploaded by a run of the pull request's own code, so they are treated as
# hostile: only image names matching a strict pattern, known outcome words and hex commit ids are
# taken from them, and the comment text is built here. Nothing downloaded is ever executed.
#
# Expects, from the workflow_run event: REPO, RUN_ID, RUN_URL, RUN_NAME, CONCLUSION, HEAD_SHA,
# HEAD_OWNER and HEAD_BRANCH, plus GH_TOKEN for the gh CLI.
set -euo pipefail

: "${REPO:?}" "${RUN_ID:?}" "${RUN_URL:?}" "${RUN_NAME:?}" "${CONCLUSION:?}"
: "${HEAD_SHA:?}" "${HEAD_OWNER:?}" "${HEAD_BRANCH:?}"
WORK_DIR="${WORK_DIR:-${RUNNER_TEMP:-/tmp}/screenshot-comment}"
MAX_LISTED=30
MAX_ARTIFACT_BYTES=1000000

case "$RUN_NAME" in
  "Screenshot Comparison") key=desktop; title="Desktop screenshot changes"; platform_emoji="🖥️" ;;
  "Android Screenshot Test") key=android; title="Android screenshot changes"; platform_emoji="📱" ;;
  *) echo "Not a screenshot workflow: $RUN_NAME"; exit 0 ;;
esac

if ! [[ "$HEAD_SHA" =~ ^[0-9a-f]{40}$ ]]; then
  echo "::error::Unexpected head sha: $HEAD_SHA"
  exit 1
fi

marker="<!-- jme-screenshot-comment:$key -->"
short_sha=${HEAD_SHA:0:7}
mkdir -p "$WORK_DIR"

# Only a pull request whose head is still this exact commit is updated. That also covers a newer
# commit having been pushed meanwhile: its own run will report, and this older one must not
# overwrite that.
pull_requests=$(gh api -X GET "repos/$REPO/pulls" -f state=open -f head="$HEAD_OWNER:$HEAD_BRANCH" \
  -f per_page=100 --jq ".[] | select(.head.sha == \"$HEAD_SHA\") | .number")
if [ -z "$pull_requests" ]; then
  echo "No open pull request has $HEAD_SHA at its head (closed, or a newer commit was pushed); nothing to do."
  exit 0
fi

existing_comment_id() {
  local ids
  ids=$(gh api "repos/$REPO/issues/$1/comments" --paginate \
    --jq ".[] | select(.user.login == \"github-actions[bot]\" and (.body | contains(\"$marker\"))) | .id")
  printf '%s' "${ids%%$'\n'*}"
}

# Updates this workflow's comment on every matching pull request. With "create" it also posts one
# where there is none yet; without, it only touches a comment that already exists, so that a pull
# request nothing ever changed on never gets one.
publish() {
  local mode="$1" body_file="$2" pr id
  for pr in $pull_requests; do
    id=$(existing_comment_id "$pr")
    if [ -n "$id" ]; then
      jq -n --rawfile body "$body_file" '{body: $body}' \
        | gh api -X PATCH "repos/$REPO/issues/comments/$id" --input - > /dev/null
      echo "Updated comment $id on #$pr"
    elif [ "$mode" = "create" ]; then
      jq -n --rawfile body "$body_file" '{body: $body}' \
        | gh api -X POST "repos/$REPO/issues/$pr/comments" --input - > /dev/null
      echo "Created a comment on #$pr"
    else
      echo "Nothing to report on #$pr and no earlier comment to update."
    fi
  done
}

body="$WORK_DIR/body.md"

case "$CONCLUSION" in
  success|failure) ;;
  action_required)
    # A first-time contributor's run waiting for approval. Results for an earlier commit would now
    # be misleading, so an existing comment says so; once the run is approved and finishes, this
    # workflow runs again with its real conclusion.
    printf '%s\n### 🖼️ %s\n\n⏳ The screenshot comparison for `%s` is waiting for a maintainer to approve the workflow run. Earlier results have been removed because they were for an older commit.\n' \
      "$marker" "$title" "$short_sha" > "$body"
    publish update "$body"
    exit 0
    ;;
  *)
    echo "Run ended as '$CONCLUSION'; leaving any comment as it is."
    exit 0
    ;;
esac

artifacts=$(gh api "repos/$REPO/actions/runs/$RUN_ID/artifacts" --paginate \
  --jq '.artifacts[] | select(.expired | not) | "\(.name)\t\(.id)\t\(.size_in_bytes)"')

artifact_id() {
  printf '%s\n' "$artifacts" | awk -F'\t' -v name="$1" '$1 == name { print $2; exit }'
}

valid_image_names() {
  grep -E '^[A-Za-z0-9_.-]{1,200}\.png$' "$1" 2>/dev/null || true
}

image_table() {
  local heading="$1" names="$2" count
  count=$(printf '%s\n' "$names" | grep -c .)
  echo "| $heading |"
  echo "| --- |"
  printf '%s\n' "$names" | head -n "$MAX_LISTED" | sed 's/.*/| `&` |/'
  if [ "$count" -gt "$MAX_LISTED" ]; then
    echo "| ...and $((count - MAX_LISTED)) more |"
  fi
  echo
}

sections="$WORK_DIR/sections.md"
: > "$sections"
total_changed=0
total_new=0
total_mismatched=0
incomplete=0
unsupported=0
found=0

while IFS=$'\t' read -r name id size; do
  [ -n "$name" ] || continue
  [[ "$name" =~ ^screenshot-results-([a-z0-9]+(-[a-z0-9]+)*)$ ]] || continue
  suffix=${BASH_REMATCH[1]}
  if [ "$size" -gt "$MAX_ARTIFACT_BYTES" ]; then
    echo "::warning::Skipping $name: $size bytes is far more than a result should be."
    continue
  fi

  dir="$WORK_DIR/artifacts/$name"
  rm -rf "$dir"
  mkdir -p "$dir"
  if ! gh run download "$RUN_ID" -R "$REPO" -n "$name" -D "$dir"; then
    echo "::warning::Could not download $name"
    continue
  fi
  found=$((found + 1))

  if [[ "$suffix" == *-* ]]; then
    label="${suffix%%-*} (${suffix#*-})"
    report_artifact="screenshot-test-report-${suffix#*-}"
    report_file="ScreenshotDiffReport.html"
  else
    label="$suffix"
    report_artifact="$suffix-screenshot-report"
    report_file="extentReport/index.html"
  fi

  outcome=$(head -n 1 "$dir/outcome" 2>/dev/null || true)
  [[ "$outcome" =~ ^(success|failure|cancelled|skipped|unsupported)$ ]] || outcome=unknown
  reference=$(head -n 1 "$dir/reference_ref" 2>/dev/null || true)
  [[ "$reference" =~ ^[0-9a-f]{40}$ ]] || reference=""
  changed=$(valid_image_names "$dir/changed.txt")
  new=$(valid_image_names "$dir/new.txt")
  changed_count=$(printf '%s' "$changed" | grep -c . || true)
  new_count=$(printf '%s' "$new" | grep -c . || true)
  mismatched=$(valid_image_names "$dir/scenario_mismatch.txt")
  mismatched_count=$(printf '%s' "$mismatched" | grep -c . || true)
  total_mismatched=$((total_mismatched + mismatched_count))
  total_changed=$((total_changed + changed_count))
  total_new=$((total_new + new_count))

  {
    if [ -n "$reference" ] && [ "$outcome" != "unsupported" ]; then
      echo "#### $platform_emoji $label - compared against \`${reference:0:7}\`"
    else
      echo "#### $platform_emoji $label"
    fi
    echo
    if [ "$outcome" = "unsupported" ]; then
      unsupported=$((unsupported + 1))
      echo "ℹ️ **Not compared:** \`${reference:0:7}\` predates generating reference images in CI, so there is nothing to compare against."
      echo
    elif [ "$outcome" != "success" ] && [ "$mismatched_count" -eq 0 ]; then
      incomplete=$((incomplete + 1))
      echo "❌ **The comparison did not run to the end**, so this list may be incomplete. See the [workflow run]($RUN_URL)."
      echo
    fi
    if [ "$outcome" = "success" ] && [ "$changed_count" -eq 0 ] && [ "$new_count" -eq 0 ] && [ "$mismatched_count" -eq 0 ]; then
      echo "✔️ No differences."
      echo
    fi
    if [ "$mismatched_count" -gt 0 ]; then
      image_table "🚨 $mismatched_count rendered differently between scenarios" "$mismatched"
    fi
    if [ "$changed_count" -gt 0 ]; then
      image_table "🔄 $changed_count changed" "$changed"
    fi
    if [ "$new_count" -gt 0 ]; then
      image_table "✨ $new_count new (no reference to compare against)" "$new"
    fi
    report_id=$(artifact_id "$report_artifact")
    if [ -n "$report_id" ]; then
      echo "📄 **Report:** [download $report_artifact]($RUN_URL/artifacts/$report_id), then open \`$report_file\` from the zip. ⚠️ The report is produced by this pull request's code, so only open it if you trust the pull request."
      echo
    fi
  } >> "$sections"
done <<< "$artifacts"

failed=0
if [ "$CONCLUSION" = "failure" ] || [ "$incomplete" -gt 0 ] || [ "$found" -eq 0 ]; then
  failed=1
fi

{
  echo "$marker"
  echo "### 🖼️ $title"
  echo
  if [ "$failed" -eq 1 ]; then
    if [ "$total_mismatched" -gt 0 ] && [ "$incomplete" -eq 0 ] && [ "$found" -gt 0 ]; then
      echo "🚨 **Serious visual failure** for \`$short_sha\` ([workflow run]($RUN_URL)). Tests whose scenarios must render identically rendered differently. Unlike the changes a reviewer can confirm, this is never an intended result, and it has to be fixed."
    else
      echo "❌ **The screenshot comparison failed** for \`$short_sha\` ([workflow run]($RUN_URL)). This is a crash or a build failure rather than a visual difference, so it is not known whether any screenshots changed. It needs fixing before these results can be relied on."
    fi
    echo
  fi
  if [ $((total_changed + total_new)) -gt 0 ]; then
    if [ "$failed" -eq 0 ]; then
      echo "ℹ️ The purpose of these tests is to report when visual changes are introduced by a PR. If these"
      echo "visual changes are intended, no action is required."
      echo
    fi
    echo "🔍 Results for \`$short_sha\` ([workflow run]($RUN_URL)). Each report shows the reference image,"
    echo "the new image and a diff side by side; artifact downloads need you to be signed in to GitHub."
  elif [ "$unsupported" -gt 0 ]; then
    echo "ℹ️ Screenshots were not compared as of \`$short_sha\` ([workflow run]($RUN_URL)), because the branch this merges into cannot generate reference images yet."
  elif [ "$failed" -eq 0 ]; then
    echo "✅ No screenshot differences found as of \`$short_sha\` ([workflow run]($RUN_URL))."
  fi
  echo
  if [ "$found" -eq 0 ]; then
    echo "⚠️ **No results were uploaded**, so the comparison failed before it could report anything. See the workflow run."
    echo
  fi
  cat "$sections"
  if [ "$total_changed" -gt 0 ]; then
    echo "⚠️ **If you didn't expect to change anything visual**, look at the report to see what changed and fix your"
    echo "changes so that these images are no longer listed."
    echo
  fi
  echo "📚 See the [screenshot test README](https://github.com/$REPO/blob/master/jme3-screenshot-tests/README.md) for more information."
} > "$body"

if [ $((total_changed + total_new + unsupported + failed)) -gt 0 ]; then
  publish create "$body"
else
  publish update "$body"
fi
