---
trigger: manual
description: This rule defines how the AI agent should manage and utilize memory improve coding consistency.
---


# AI Memory Rule

This rule defines how the AI should manage and utilize its "memory" regarding this specific project, including user preferences, learned facts, and project-specific conventions.

## Purpose

The AI's memory helps maintain consistency and adapt to specific project needs or user preferences discovered during interactions. It prevents the AI from repeatedly asking for the same information or making suggestions contrary to established patterns.

## Storage

All learned project-specific knowledge and preferences should be stored and referenced in the `learned-memories.mdc` file located in `.cursor/rules`.

## Updating Memory

When new information relevant to the project's conventions, user preferences, or specific technical details is learned (either explicitly told by the user or inferred through conversation), the AI should:

1.  **Identify Key Information:** Determine the core piece of knowledge to be stored.
2.  **Check Existing Memory:** Review `learned-memories.mdc` to see if this information contradicts or updates existing entries.
3.  **Propose Update:** Suggest an edit to `learned-memories.mdc` to add or modify the relevant information. Keep entries concise and clear.

## Using Memory

Before proposing solutions, code changes, or answering questions, the AI should consult `learned-memories.mdc` to ensure its response aligns with the recorded knowledge and preferences.

## Example Scenario

**User:** "We've decided to use Tailwind v4 for this project, not v3."

**AI Action:**

1.  Recognize this as a project-specific technical decision.
2.  Check `learned-memories.mdc` for existing Tailwind version information.
3.  Propose adding or updating an entry in `learned-memories.mdc`:
    ```markdown
    ## Technical Decisions

    *   **CSS Framework:** Tailwind v4 is used. Ensure usage aligns with v4 documentation and practices, noting differences from v3.
    ```
4.  In subsequent interactions involving Tailwind, the AI will refer to this entry and consult v4 documentation if necessary.

## Memory File (`.cursor/rules/learned-memories.mdc`)

The basic structure:

```markdown
# Project Memory

This file stores project-specific knowledge, conventions, and user preferences learned by the AI assistant.

## User Preferences

-   [Preference 1]
-   [Preference 2]

## Technical Decisions

-   [Decision 1]
-   [Decision 2]

## Project Conventions

-   [Convention 1]
-   [Convention 2]
```
