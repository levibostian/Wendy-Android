## [0.1.1-alpha] - 2018-04-13
### Added 
- Add `strict` mode to help developer during development of Wendy.

### Fixed 
- Update README.md docs for the new changes done to the library recently.
- PendingTasks no longer get deleted on failing running tasks.

### Changed
- **Breaking Change** This release comes with a SQLite database change but does *not* come with a database migration! This is not backwards compatible with the last release `0.1.0-alpha`. This was done because `0.1.0-alpha` is not used in production by any apps (to my knowledge) and the change would have taken a bit of work to do. This was the easier solution for the situation. 
- **Breaking Change** `PendingTasksFactory.getTask()` returns nullable `PendingTask`.
- **Breaking Change** Make `taskId` property nullable in `PendingTask` as it is null until it is added to Wen$
- **Breaking Change** Rename `PendingTasks` file to `Wendy`.
- Do not allow tasks to run if part of a group and the task is not the first task in a gr$
