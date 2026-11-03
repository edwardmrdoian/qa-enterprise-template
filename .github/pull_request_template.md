## What does this PR add or change?

## Checklist
- [ ] Tests are tagged (`smoke` / `regression` and `ui` / `api` / `db` / `integration`)
- [ ] No `Thread.sleep`, no hard-coded secrets, no shared mutable state between tests
- [ ] Test data is created by the test and cleaned up by the test
- [ ] `./mvnw clean verify` passes locally (or `.\mvnw.cmd clean verify` on Windows)
- [ ] Optional integration/external suites run when relevant; record commands and results below
- [ ] Framework checks stay deterministic; examples are explicitly selected, not silently skipped
- [ ] README / docs updated if behavior or commands changed

## Validation
<!-- List exact commands with passed/failed results. Explain anything not run. -->
