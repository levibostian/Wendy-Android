### [0.2.0-alpha] 2018-06-11

### Added
- Method added to stop the task runner and delete all Wendy databases and shared preferences. Used to "reset" Wendy if your app user decides to logout of the app, for example.
- README.md docs have been improved to include how to provide dependencies to your `PendingTask` instances to perform tasks such as API calls and working with a database.

### Changed
- Compile version for library updated to SDK 28 (Android P).

## [0.1.3-alpha] - 2018-05-02
### Fixed
- When adding a TaskStatusListener for a PendingTask, check if there is an error for the PendingTask and call the listener if there is.
- Improve logic for handling PendingTasks when one is added to Wendy but the task runner is already running a similar task.

### Changed
- When `Wendy.resolveError(taskId)` is called for a PendingTask that belongs to a group, Wendy will grab all of the *similar* PendingTasks of that group (same tag, dataId, groupId), iterate them all in order by createdAt property, then resolve the first error that Wendy comes upon. This is to help the developer not need to handle 2+ different PendingTask taskIds for groups. 

## [0.1.2-alpha] - 2018-04-18
### Added
- Enforce a new best practice: All subclasses of a PendingTask must all have a groupId or none of them have a groupId.

### Fixed
- While a PendingTask is running by the task runner, if a duplicate of that PendingTask gets added to Wendy, do not delete the PendingTask if it runs successfully.
- I forgot to include all of the parameters in the recursion calls to the task runner's runAllTasks() function call. That's fixed. 

### Changed
- **Breaking Change** Removed the `rescheduled` parameter in the Wendy listeners when a task is complete. It is always rescheduled if it fails so no need for the paramter.
- **Breaking Change** `Wendy.runAllTasks()` now takes an object for filtering instead of a string for `groupId`. 

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
