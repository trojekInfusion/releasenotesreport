# release notes generator

## release notes generator is java program for creating release notes from git repository history

## Steps of work

1. Building configuration either from cli parameters and/or from given properties configuration file
2. Getting git log messages limited by tag(s) or commit id(s)
3. Matching jira issue ids from git messages based on given pattern
4. Creating report in html
5. Pushing generated report to git remote repository