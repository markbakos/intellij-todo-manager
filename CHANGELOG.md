# To-Do Task Manager for IntelliJ IDEs

## [1.11.1] - 2025-07-25

### Fixed

- Fixed some codes in Serbian using Cyrillic letters
- Added missing language codes

## [1.11.0] - 2025-07-22

### Added

- Added "Copy Tasks To Clipboard" button to Settings menu, allowing users to copy tasks.json content to clipboard 

## [1.10.0] - 2025-07-17

### Added

- Added option to extract all todo comments to a file or add to task manager
- Added "Reload Tasks" button to Settings menu

## [1.9.1] - 2025-07-15

### Fixed

- Russian now uses Cyrillic letters
- Removed deprecated methods
- Fixed comments now being automatically added "https://" if not provided, to avoid error

## [1.9.0] - 2025-07-11

### Changed

- Changed Tag Manager to be fully dynamic, no longer uses/needs tags.json to keep track of tags, instead it gets tags from tasks.json

## [1.8.3] - 2025-07-09

### Added

- Added Dutch Language Bundle
- Added Russian Language Bundle
- Added Portuguese Language Bundle
- Added Italian Language Bundle
- Added Spanish Language Bundle

## [1.8.2] - 2025-07-07

### Fixed

- Fixed issue with language bundles having trouble with special characters

## [1.8.1] - 2025-07-03

### Added

- Added Polish Language Bundle
- Added Serbian Language Bundle
- Added Hungarian Language Bundle
- Added Slovak Language Bundle

### Fixed

- Fixed issue with changing language causing infinite loop that causes freezing

## [1.8.0] - 2025-07-01

### Added

- Added Internationalization support, allowing the plugin to be translated to different languages
- Added French Language Bundle
- Added German Language Bundle
- Added Settings tab

## [1.7.1] - 2025-06-26

### Added

- Added "View Prerequisite Task" as a dynamic button to context menu, opens the tab and selects the prerequisite task

## [1.7.0] - 2025-06-24

### Added

- Added Prerequisite Task feature to tasks

### Changed

- Changed context menu to be dynamic, showing only relevant actions based on the selected task

## [1.6.2] - 2025-06-22

### Changed

- Changed TODO comment tracking from Line Number to Text Content, allowing tracking of comments when line numbers change
- Changed plugin icon

## [1.6.1] - 2025-06-17

### Added

- Added new Open Comment Location for Tasks imported from TODO comments, navigates to file and line it was imported from

### Fixed

- Fixed issue with multi-line comments not being extracted correctly

## [1.6.0] - 2025-06-15

### Added

- Tasks imported from TODO comments now save the file and line of the original comment

## [1.5.5] - 2025-06-12

### Added

- Finish date to tasks, when a task's status get changed to "DONE", automatically records the finish date and time and displays it in Edit Dialog.

## [1.5.4] - 2025-06-09

### Changed

- Updated plugin icon

### Fixed

- Made Edit Task Dialog height bigger to fix issues with components being too small

### Removed

- Removed the need for counter.json, instead get unique ID from tasks.json

## [1.5.3] - 2025-06-04

### Added

- Added formatted creation date and time to Edit Task Dialog

### Fixed

- Fixed issue with sorting not being saved when table gets refreshed

## [1.5.2] - 2025-05-28

### Added

- Added creation date and time to tasks

### Changed

- Changed plugin support until ide build 2026.1.*

## [1.5.1] - 2025-03-23

### Added

- Added support for converting TODO comments to tasks

### Changed

- Increased the sizes of the Add Task and Edit Task Dialogs

## [1.5.0] - 2025-03-20

### Added

- Added Support for links

### Fixed

- Fixed "Change Status" displaying wrong status
- Fixed issue with refreshing tabs always returning to first tab

## [1.4.0] - 2025-03-16

### Added

- Filtering by tags
- Context menu on right click for actions

### Changed

- Refactored code for better organization
- Added function to get selected task index
- Added color support for dark themes

## [1.3.0] - 2025-03-12

### Added

- Added a complete README
- Added Plugin Icon

### Changed

- Reworked the Tag System, with new dialog for adding/remove tags

### Fixed

- Fixed bug with "Edit Task" and "Add New Task" Dialog going behind other windows

## [1.2.0] - 2025-03-07

### Added

- Added sorting by priority
- Added text colors to priority

### Changed

- Changed plugin support until build 2025.1.*
- Changed order of columns
- Changed default size of columns

### Removed

- Removed title column completely

## [1.1.0] - 2025-03-06

### Added

- Saving and Loading tasks to a JSON file in /.todo
- Saving and Loading ID Counter in /.todo
- Added buttons for deleting, editing and changing the status of existing tasks.

### Changed

- Disabled resizing the Add Task Dialog Window
- Organized codebase

### Fixed

- Disabled user cell editing, double click now opens the edit dialog

### Removed

- Removed ID column visually from the table

## [1.0.0] - 2025-03-05

### Added

- Basic functionality
- Adding tasks

[1.11.1]:  https://github.com/markbakos/intellij-todo-manager/
[1.11.0]:  https://github.com/markbakos/intellij-todo-manager/
[1.10.0]: https://github.com/markbakos/intellij-todo-manager/
[1.9.1]: https://github.com/markbakos/intellij-todo-manager/
[1.9.0]: https://github.com/markbakos/intellij-todo-manager/
[1.8.3]: https://github.com/markbakos/intellij-todo-manager/
[1.8.2]: https://github.com/markbakos/intellij-todo-manager/
[1.8.1]: https://github.com/markbakos/intellij-todo-manager/
[1.8.0]: https://github.com/markbakos/intellij-todo-manager/
[1.7.1]: https://github.com/markbakos/intellij-todo-manager/
[1.7.0]: https://github.com/markbakos/intellij-todo-manager/
[1.6.2]: https://github.com/markbakos/intellij-todo-manager/
[1.6.1]: https://github.com/markbakos/intellij-todo-manager/
[1.6.0]: https://github.com/markbakos/intellij-todo-manager/
[1.5.5]: https://github.com/markbakos/intellij-todo-manager/
[1.5.4]: https://github.com/markbakos/intellij-todo-manager/
[1.5.2]: https://github.com/markbakos/intellij-todo-manager/
[1.5.3]: https://github.com/markbakos/intellij-todo-manager/
[1.5.2]: https://github.com/markbakos/intellij-todo-manager/
[1.5.1]: https://github.com/markbakos/intellij-todo-manager/
[1.5.0]: https://github.com/markbakos/intellij-todo-manager/
[1.4.0]: https://github.com/markbakos/intellij-todo-manager/
[1.3.0]: https://github.com/markbakos/intellij-todo-manager/
[1.2.0]: https://github.com/markbakos/intellij-todo-manager/
[1.1.0]: https://github.com/markbakos/intellij-todo-manager/
[1.0.0]: https://github.com/markbakos/intellij-todo-manager/