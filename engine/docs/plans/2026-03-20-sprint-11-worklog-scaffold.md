# Sprint-11 Worklog Scaffold Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create the standard `worklog/v1.0.0/sprint-11` scaffold for the next sprint without disturbing in-progress code changes.

**Architecture:** Add only markdown files and a tracked empty directory marker under `worklog/v1.0.0/sprint-11`. Keep sprint metadata intentionally generic so later work can fill in the actual sprint theme and task list.

**Tech Stack:** Markdown, git-tracked directory structure

---

### Task 1: Add Sprint-11 planning records

**Files:**
- Create: `docs/plans/2026-03-20-sprint-11-worklog-scaffold-design.md`
- Create: `docs/plans/2026-03-20-sprint-11-worklog-scaffold.md`

**Step 1: Write the design summary**

Create a short design note that explains:
- why `sprint-11` is being added
- which files/directories are part of the scaffold
- which existing changes must remain untouched

**Step 2: Write the implementation plan**

Create this plan with exact output paths and verification commands.

**Step 3: Verify files exist**

Run: `find /opt/prod/prs/source/dts-copilot/docs/plans -maxdepth 1 -name '2026-03-20-sprint-11-worklog-scaffold*' | sort`
Expected: both new plan files are listed.

### Task 2: Create the sprint root README

**Files:**
- Create: `worklog/v1.0.0/sprint-11/README.md`

**Step 1: Add the sprint metadata**

Include:
- `Sprint-11` title
- prefix
- status
- goal
- background
- task list placeholder
- completion checklist

**Step 2: Keep content intentionally generic**

Do not assume the sprint business theme yet. Use placeholder planning text only.

**Step 3: Verify the file**

Run: `sed -n '1,220p' /opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-11/README.md`
Expected: the README renders with the same top-level structure as existing sprint READMEs.

### Task 3: Create IT and tasks scaffold

**Files:**
- Create: `worklog/v1.0.0/sprint-11/it/README.md`
- Create: `worklog/v1.0.0/sprint-11/tasks/.gitkeep`

**Step 1: Add IT README**

Document that the directory is reserved for:
- acceptance matrix
- test execution plan
- result records

**Step 2: Track the empty tasks directory**

Add `.gitkeep` only so the empty `tasks/` directory stays in git until real task docs are added.

**Step 3: Verify the scaffold**

Run: `find /opt/prod/prs/source/dts-copilot/worklog/v1.0.0/sprint-11 -maxdepth 2 | sort`
Expected: root README, `it/README.md`, and `tasks/.gitkeep` are present.

### Task 4: Final verification

**Files:**
- Verify: `worklog/v1.0.0/sprint-11/**`

**Step 1: Check working tree impact**

Run: `git -C /opt/prod/prs/source/dts-copilot status --short`
Expected: only the new scaffold files appear in addition to any pre-existing user changes.

**Step 2: Confirm no unrelated files were modified**

Review the paths shown by `git status --short`.

**Step 3: Hand off**

Report the created paths and note that sprint content is still a placeholder until tasks are defined.

