# Project session instructions

## Read first in every new session

1. Read `docs/BACKEND_IMPLEMENTATION_PLAN.md` before planning or changing this project. Start with its **Session checkpoint**, then the relevant milestone.
2. Check the current working tree and relevant source files; do not assume a previous session's status is still accurate. Preserve unrelated uncommitted work.
3. Follow the latest user instructions and screenshot reference notes. The backend plan is the delivery tracker; older UI milestones and architecture proposals are not proof of completion.
4. At the end of backend work, update the plan's checkpoint, milestone status, decisions, changed files, validation evidence, and exact next action. Keep implemented and verified status separate.
5. The user subsequently authorized implementation with "start implementation". Continue backend work from the plan's checkpoint, one bounded milestone at a time. The existing build-only verification restriction remains in effect until the user changes it.

## graphify

- **graphify** (`~/.Codex/skills/graphify/SKILL.md`) turns input into a knowledge graph. Trigger: `/graphify`.
- When the user types `/graphify`, invoke the graphify skill before doing anything else, using the available skill-loading mechanism.
