# HOW TO CONTRIBUTE

We are always very happy to have contributions, whether for trivial cleanups or big new features.

If you don't know Java or Scala you can still contribute to the project. 
Code is not the only way to contribute to the project. We strongly value documentation and gladly accept improvements to the documentation.

# REPORTING AN ISSUE

Reporting potential issues as Github issues is more than welcome as a significant contribution to the project. But please be aware that Github issues should not be used for FAQs: 
if you have a question or are simply not sure if it is really an issue or not, please contact us ([through gitter](https://gitter.im/agile-lab-darwin/community)) first before you create a new issue.
 
# CONTRIBUTING A CODE CHANGE

To submit a change for inclusion, please do the following:
- If the change is non-trivial please include some unit tests that cover the new functionality.
- If you are introducing a completely new feature or API it is a good idea to start a markdown description in the Github issue itself and get consensus on the basic design first.
- Make sure you have observed the recommendations in the style guide. (scalastyle:check should pass with no errors or warnings)
- Follow the detailed instructions in Contributing Code Changes.

## Contributing code changes

### Overview

Generally, Darwin uses:
- Github issues to track logical issues, including bugs and improvements
- Github pull requests to manage the review and merge of specific code changes

### Github issues

- Find the existing Github issue that the change pertains to.
- Do not create a new issue if creating a change to address an existing issue in Github; add to the existing discussion and work instead.
- To avoid conflicts, assign the Github issue to yourself if you plan to work on it.
- Look for existing pull requests that are linked to the issue, to understand if someone is already working on it.
- If required, create a new issue (below shows some critical fields to fill-in):
  - Provide a descriptive Title. "Update web UI" or "Problem in scheduler" is not sufficient. "Support NiFi SchemaRegistry interface and add meta-connector" is good.
  - Write a detailed Description. For bug reports, this should ideally include a short reproduction of the problem. For new features, it may include a design document. 
  - To avoid conflicts, assign the issue to yourself if you plan to work on it. Leave it unassigned otherwise.
  - Do not include a patch file; pull requests are used to propose the actual change.
  - If the change is a large change, consider inviting discussion on the issue on gitter first before proceeding to implement the change. 


### Pull Request

- Fork the Github repository at if you haven't already 
- Clone your fork, create a new branch, push commits to the branch.
- Consider whether documentation or tests need to be added or updated as part of the change, and add them as needed (doc changes should be submitted along with code change in the same PR).
- Run all tests using `make.sh` script.
- Open a pull request against the develop branch.
- The PR title should usually be of the form [#issue-number]: Title, where [#issue number] is the relevant Github issue number and Title may be the issue title or a more specific title describing the PR itself.
- If the pull request is still a work in progress, and so is not ready to be merged, but needs to be pushed to Github to facilitate review, use the draft mode of Github PR..
- Consider identifying committers or other contributors who have worked on the code being changed. The easiest is to simply follow GitHub's automatic suggestions. You can add @username in the PR description to ping them immediately.
- Once ready, the PR `checks` box will be updated.
- Investigate and fix failures caused by the pull the request
- Fixes can simply be pushed to the same branch from which you opened your pull request.
- Please address feedback via additional commits instead of amending existing commits. This makes it easier for the reviewers to know what has changed since the last review. All commits will be squashed into a single one by the committer via GitHub's squash button or by a script as part of the merge process.
- CI will automatically re-test when new commits are pushed.
- Despite our efforts, Darwin may have flaky tests at any given point, which may cause a build to fail. You need to ping committers to trigger a new build. If the failure is unrelated to your pull request and you have been able to run the tests locally successfully, please mention it in the pull request.

### The Review Process

- Other reviewers, including committers, may comment on the changes and suggest modifications. Changes can be added by simply pushing more commits to the same branch.
- Please add a comment and "@" the reviewer in the PR if you have addressed reviewers' comments. Even though GitHub sends notifications when new commits are pushed, it is helpful to know that the PR is ready for review once again.
- Lively, polite, rapid technical debate is encouraged from everyone in the community. The outcome may be a rejection of the entire change.
- Reviewers can indicate that a change looks suitable for merging by approving it via GitHub's review interface. This indicates the strongest level of technical sign-off on a patch and it means: "I've looked at this thoroughly and take as much ownership as if I wrote the patch myself". If you approve a pull request, you will be expected to help with bugs or follow-up issues on the patch. Consistent, judicious use of pull request approvals is a great way to gain credibility as a reviewer with the broader community. Darwin reviewers will typically include the acronym LGTM in their approval comment. This was the convention used to approve pull requests before the "approve" feature was introduced by GitHub.
- Sometimes, other changes will be merged which conflict with your pull request's changes. The PR can't be merged until the conflict is resolved. This can be resolved with "git fetch origin" followed by "git merge origin/develop" and resolving the conflicts by hand, then pushing the result to your branch.
- Try to be responsive to the discussion rather than let days pass between replies.

### Closing Your Pull Request / issue

- If a change is accepted, it will be merged and the pull request will automatically be closed, along with the associated issue if any
- If your pull request is ultimately rejected, please close it.
- If a pull request has gotten little or no attention, consider improving the description or the change itself and ping likely reviewers again after a few days. Consider proposing a change that's easier to include, like a smaller and/or less invasive change.
- If a pull request is closed because it is deemed not the right approach to resolve an issue, then leave the issue open. However if the review makes it clear that the issue identified in the issue is not going to be resolved by any pull request (not a problem, won't fix) then also resolve the issue. 

*This document is heavily inspired by Kafka/Apache contribution guidelines.*
