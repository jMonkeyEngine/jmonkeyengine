# How to contribute to jMonkeyEngine

First and foremost, you have to familiarize yourself with Git & GitHub. Dig through 
[help.github.com](https://help.github.com/), [try.github.io](http://try.github.io/) and the [gh cheat sheet](https://github.com/tiimgreen/github-cheat-sheet/blob/master/README.md) if these are new topics for you. If you'd like to contribute with something other than code, just tell us about it on the forum.

## Communication

Communication always comes first. **All** code changes and other contributions should start with the [forum](http://hub.jmonkeyengine.org/). Make a thread to explain your change and show us the important bits of your code. If the code is too long to be posted within the forum’s code tags, please paste your code in a Gist or pastebin and link to the submission in your thread. You are required to register on our website in order to create threads. (We do support login via GitHub though).

### New Contributors

Check out the [Projects](https://github.com/jMonkeyEngine/jmonkeyengine/projects/1) tab, where the team has prioritized issues that you as a new contributor can undertake that will familiarize you to the workflow of contributing. This highlights some issues the team thinks would be a good start for new contributors but you are free to contribute on any other issues or integration you wish.

When you're ready to submit your code, just make a [pull request](https://help.github.com/articles/using-pull-requests).

- In your commit log message, please refer back to the originating forum thread (example) for a ‘full circle’ reference. Also please [reference related issues](https://help.github.com/articles/closing-issues-via-commit-messages) by typing the issue hashtag.
- When committing, always be sure to run an update before you commit. If there is a conflict between the latest revision and your patch after the update, then it is your responsibility to track down the update that caused the conflict and determine the issue (and fix it). In the case where the breaking commit has no thread linked (and one cannot be found in the forum), then the contributor should contact an administrator and wait for feedback before committing.
- If your code is committed and it introduces new functionality, please edit the wiki accordingly. We can easily roll back to previous revisions, so just do your best; point us to it and we’ll see if it sticks!

p.s. We will try hold ourselves to a [certain standard](http://www.defmacro.org/2013/04/03/issue-etiquette.html) when it comes to GitHub etiquette. If at any point we fail to uphold this standard, let us know.

There are many ways
to submit a pull request (PR) to the "jmonkeyengine" project repository,
depending on your knowledge of Git and which tools you prefer.

<details>
    <summary>
        <b>Click to view step-by-step instructions for a reusable setup
        using a web browser and a command-line tool such as Bash.</b>
    </summary>

The setup described here allows you to reuse the same local repo for many PRs.

#### Prerequisites

These steps need only be done once...

1. You'll need a personal account on https://github.com/ .
   The "Sign up" and "Sign in" buttons are in the upper-right corner.
2. Create a GitHub access token, if you don't already have one:
  + Browse to https://github.com/settings/tokens
  + Click on the "Generate new token" button in the upper right.
  + Follow the instructions.
  + When specifying the scope of the token, check the box labeled "repo".
  + Copy the generated token to a secure location from which you can
    easily paste it into your command-line tool.
3. Create your personal fork of the "jmonkeyengine" repository at GitHub,
   if you don't already have one:
  + Browse to https://github.com/jMonkeyEngine/jmonkeyengine
  + Click on the "Fork" button (upper right)
  + Follow the instructions.
  + If offered a choice of locations, choose your personal account.
4. Clone the fork to your development system:
  + `git clone https://github.com/` ***yourGitHubUserName*** `/jmonkeyengine.git`
  + As of 2021, this step consumes about 1.3 GBytes of filesystem storage.
5. Create a local branch for tracking the project repository:
  + `cd jmonkeyengine`
  + `git remote add project https://github.com/jMonkeyEngine/jmonkeyengine.git`
  + `git fetch project`
  + `git checkout -b project-master project/master`

#### PR process

1. Create a temporary, up-to-date, local branch for your PR changes:
  + `git checkout project-master`
  + `git pull`
  + `git checkout -b tmpBranch` (replace "tmpBranch" with a descriptive name)
2. Make your changes in the working tree.
3. Test your changes.
   Testing should, at a minimum, include building the Engine from scratch:
  + `./gradlew clean build`
4. Add and commit your changes to your temporary local branch.
5. Push the PR commits to your fork at GitHub:
  + `git push --set-upstream origin ` ***tmpBranchName***
  + Type your GitHub user name at the "Username" prompt.
  + Paste your access token (from prerequisite step 2) at the "Password" prompt.
6. Initiate the pull request:
  + Browse to [https://github.com/ ***yourGitHubUserName*** /jmonkeyengine]()
  + Click on the "Compare & pull request" button at the top.
  + The "base repository:" should be "jMonkeyEngine/jmonkeyengine".
  + The "base:" should be "master".
  + The "head repository:" should be your personal fork at GitHub.
  + The "compare:" should be the name of your temporary branch.
7. Fill in the textboxes for the PR name and PR description, and
    click on the "Create pull request" button.

To amend an existing PR:
  + `git checkout tmpBranch`
  + Repeat steps 2 through 5.

To submit another PR using the existing local repository,
repeat the PR process using a new temporary branch with a different name.

If you have an integrated development environment (IDE),
it may provide an interface to Git that's more intuitive than a command line.
</details>

Generic instructions for creating GitHub pull requests can be found at
https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request

### Core Contributors

Developers in the Contributors team can push directly to Main instead of submitting pull requests, however for new features it is often a good idea to do a pull request as a means to get a last code review.

## Customs around integration, branching, tagging, and releases

- Most pull requests are integrated directly into the master branch of the repository.
- Integrators should note, unless the history of the pull request is important, it should be integrated to a single commit using “squash and merge”. If the history is important, favor “rebase and merge”. Don’t create a merge commit unless GitHub cannot rebase the PR.
- For each major release (such as v3.0 or v3.3), an appropriately named release branch is created in the repository.
- For each minor (or “dot-dot”) release (such as v3.2.3), an appropriately named tag is created in the repository.
- In general, library changes that plausibly might break existing apps appear only in major releases, not minor ones.


## How to build the Engine from source

### Prerequisites

These steps need only be done once...

1. Install a Java Development Kit (JDK), if you don't already have one.
2. Set the JAVA_HOME environment variable:
  + using Bash: `export JAVA_HOME="` *path to your JDK* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to your JDK* `'`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to your JDK* `"`
  + Tip: The path names a directory containing "bin" and "lib" subdirectories.
    On Linux it might be something like "/usr/lib/jvm/java-17-openjdk-amd64"
  + Tip: You may be able to skip this step
    if the JDK binaries are in your system path.
3. Clone the project repository from GitHub:
  + `git clone https://github.com/jmonkeyengine/jmonkeyengine.git`
  + `cd jmonkeyengine`
  + As of 2021, this step consumes about 1.3 GBytes of filesystem storage.

### Build command

Run the Gradle wrapper:
+ using Bash or PowerShell: `./gradlew build`
+ using Windows Command Prompt: `.\gradlew build`

After a successful build,
snapshot jars will be found in the "*/build/libs" subfolders.

### Related Gradle tasks

You can install the Maven artifacts to your local repository:
 + using Bash or PowerShell:  `./gradlew install`
 + using Windows Command Prompt:  `.\gradlew install`

You can restore the project to a pristine state:
 + using Bash or PowerShell: `./gradlew clean`
 + using Windows Command Prompt: `.\gradlew clean`

## Best Practices

### Git essentials

- [Creating good pull requests](http://seesparkbox.com/foundry/creating_good_pull_requests)
- [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request?utm_content=buffer0eb16&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer)

### Testing

general testing tips? WIP

### Coding Style

+ Our preferred style for Java source code is
  [Google style](https://google.github.io/styleguide/javaguide.html) with the following 8 modifications:
  1. No blank line before a `package` statement. (Section 3)
  2. Logical ordering of class contents is encouraged but not required. (Section 3.4.2)
  3. Block indentation of +4 spaces instead of +2. (Section 4.2)
  4. Column limit of 110 instead of 100. (Section 4.4)
  5. Continuation line indentation of +8 spaces instead of +4. (Section 4.5.2)
  6. Commented-out code need not be indented at the same level as surrounding code. (Section 4.8.6.1)
  7. The names of test classes need not end in "Test". (Section 5.2.2)
  8. No trailing whitespace.
+ Any pull request that adds new Java source files shall apply our preferred style to those files.
+ Any pull request that has style improvement as its primary purpose
  shall apply our preferred style, or specific aspect(s) thereof, to every file it modifies.
+ Any pull request that modifies a pre-existing source file AND
  doesn't have style improvement as it's primary purpose shall either:
  1. conform to the prevailing style of that file OR
  2. apply our preferred style, but only to the portions modified for the PR's primary purpose.

### Code Quality

Make an effort to write elegant code:

 1. Handles errors gracefully
 2. Only reinvents the wheel when there is a measurable benefit in doing so.
 3. Has consistent naming conventions.
 4. Has comments around ugly code explaining why it is ugly.
 5. Compiles (or runs if interpreted) without warnings.

## Reporting bugs

 1. Start by searching the [forum](http://hub.jmonkeyengine.org) and GH issue tracker for duplicates.
 2. Create a new issue, explaining the problem in proper detail (templates pending).

## Documentation

- How to edit the [wiki](https://github.com/jMonkeyEngine/wiki).
- How to edit JavaDocs - WIP
