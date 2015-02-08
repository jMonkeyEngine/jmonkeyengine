# How to contribute to jMonkeyEngine

First and foremost, you have to familiarize yourself with Git & GitHub. Dig through 
[help.github.com](https://help.github.com/), [try.github.io](http://try.github.io/) and the [gh cheat sheet](https://github.com/tiimgreen/github-cheat-sheet/blob/master/README.md) if these are new topics for you. If you'd like to contribute with something other than code, just tell us about it on the forum.

## Communication

Communication always comes first. **All** code changes and other contributions should start with the [forum](http://hub.jmonkeyengine.org/forum/). Make a thread to explain your change and show us the important bits of your code. If the code is too long to be posted within the forum’s code tags, please paste your code in a Gist or pastebin and link to the submission in your thread. You are required to register on our website in order to create threads.

### New Contributors

When you're ready to submit your code, just make a [pull request](https://help.github.com/articles/using-pull-requests).

- Do not commit your code until you have received proper feedback.
- In your commit log message, please refer back to the originating forum thread (example) for a ‘full circle’ reference. Also please [reference related issues](https://help.github.com/articles/closing-issues-via-commit-messages) by typing the issue hashtag.
- When committing, always be sure to run an update before you commit. If there is a conflict between the latest revision and your patch after the update, then it is your responsibility to track down the update that caused the conflict and determine the issue (and fix it). In the case where the breaking commit has no thread linked (and one cannot be found in the forum), then the contributor should contact an administrator and wait for feedback before committing.
- If your code is committed and it introduces new functionality, please edit the wiki accordingly. We can easily roll back to previous revisions, so just do your best; point us to it and we’ll see if it sticks!

**Note to Eclipse users:** The Eclipse [git client does not support https](http://hub.jmonkeyengine.org/forum/topic/problem-cloning-the-new-git-repository/#post-265594). The current workaround is to use the command line to clone the repository.
To import the local repository as a project follow these steps:

1. Add a line 'apply plugin: eclipse' to your common.gradle file in the main project directory.
2. Navigate to the project directory in command line and execute command 'gradle eclipse'. This will load all the dependancies for eclipse.
3. In Eclipse, add the repository as an existing Java Project.



#### Core Contributors

Developers in the Contributors team can push directly to Main instead of submitting pull requests, however for new features it is often a good idea to do a pull request as a means to get a last code review.

## Building the engine

1. Install [Gradle](http://www.gradle.org/)
2. Navigate to the project directory and run 'gradle build' from command line to build the engine.

## Best Practices

### Git essentials

- [Creating good pull requests](http://seesparkbox.com/foundry/creating_good_pull_requests)
- [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request?utm_content=buffer0eb16&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer)

### Testing

general testing tips? WIP

### Code Quality

We generally abide by the standard Java Code Conventions. Besides that, just make an effort to write elegant code:

 1. Handles errors gracefully
 2. Only reinvents the wheel when there is a measurable benefit in doing so.
 3. Has consistent naming conventions.
 4. Has comments around ugly code explaining why it is ugly.
 5. Compiles (or runs if interpreted) without warnings.

## Reporting bugs

 1. Start by searching the [forum](http://hub.jmonkeyengine.org) and GH issue tracker for duplicates.
 2. Create a new issue, explaining the problem in proper detail (templates pending).

## Documentation

- How to edit the wiki - WIP
- How to edit JavaDocs - WIP
