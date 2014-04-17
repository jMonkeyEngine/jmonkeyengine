# How to contribute to jMonkeyEngine

First and foremost, you have to familiarize yourself with Git & GitHub. Dig through 
[help.github.com](https://help.github.com/) and [try.github.io](http://try.github.io/) if these are new topics for you. If you'd like to contribute with something other than code, just tell us about it and we'll figure something out.

## Communication

Communication always comes first. **All** code changes and other contributions should start with the [forum](http://hub.jmonkeyengine.org/forum/). Make a thread to explain your change and show us the important bits of your code. If the code is too long to be posted within the forum’s code tags, please paste your code in a Gist or pastebin and link to the submission in your thread. You are required to register on our website in order to create threads.

### New Contributors

When you're ready to submit your code, just make a [pull request](https://help.github.com/articles/using-pull-requests).

- Do not commit your code until you have received proper feedback.
- In your commit log message, please refer back to the originating forum thread (example) for a ‘full circle’ reference. Also please [reference related issues](https://help.github.com/articles/closing-issues-via-commit-messages) by typing the issue hashtag.
- When committing, always be sure to run an update before you commit. If there is a conflict between the latest revision and your patch after the update, then it is your responsibility to track down the update that caused the conflict and determine the issue (and fix it). In the case where the breaking commit has no thread linked (and one cannot be found in the forum), then the contributor should contact an administrator and wait for feedback before committing.
- If your code is committed and it introduces new functionality, please edit the wiki accordingly. We can easily roll back to previous revisions, so just do your best; point us to it and we’ll see if it sticks!

**Note to Eclipse users:** The Eclipse [git client does not support https](http://hub.jmonkeyengine.org/forum/topic/problem-cloning-the-new-git-repository/#post-265594). The current workaround is to use the command line to clone the repository and then add local repository in Eclipse.

#### Core Contributors

Developers in the Contributors team can push directly to Main instead of submitting pull requests, however for new features it is often a good idea to do a pull request as a means to get a last code review.

## Building the engine

NEEDS ATTENTION: Gradle and whatnot.

## Best Practices

### Git essentials

rebase...

### Testing

general testing tips?

### Code Quality

We generally abide by the standard Java Code Conventions. Besides that, just make an effort to write elegant code:

 1. Handles errors gracefully
 2. Only reinvents the wheel when there is a measurable benefit in doing so.
 3. Has consistent naming conventions.
 4. Has comments around ugly code explaining why it is ugly.
 5. Compiles (or runs if interpreted) without warnings.

## Reporting bugs

 1. Start by searching the forum and issue tracker for duplicates.
 2. Create a new issue, explaining the problem in proper detail (templates pending).

## Documentation

- Edit the wiki
- Edit JavaDocs
