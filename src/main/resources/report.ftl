<#escape x as x?html>
<html>
<head>
    <title>Release notes for version ${releaseVersion}</title>
    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
</head>
<body>
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-10">
                <div class="page-header">
                    <h1>Release notes for version ${releaseVersion}</h1>
                    <h2><small>Generated for commits<span class="badge">${commitsCount}</span> from branch <strong>${gitBranch}</strong> between ${commitTag1.commit}<span class="label label-success">${(commitTag1.tag!"")}</span> and ${commitTag2.commit}<span class="label label-success">${(commitTag2.tag!"")}</span></small></h2>
	                <h2><small>
	                <#if (!fixVersions.isEmpty())>
		                <p>Fix versions: ${fixVersions}</p>
	                </#if>
	                <#if (!getFixVersionErrorMessage().isEmpty())>
		                <p>Error: ${getFixVersionErrorMessage()}</p>
	                </#if>
	                </h2></small>
                </div>
            </div>
        </div>

        <#list getIssueCategoryNamesList() as categoryName>
            <div class="row">
                <div class="col-md-8">
	                <#if (!getGenericErrorMessage().isEmpty())>
		                <p>Error: ${getGenericErrorMessage()}</p>
	                </#if>
	                <#if (getIssuesByCategoryName(categoryName).size()>0)>
		                <#if (categoryNameIsInvalid(categoryName))>
		                    <h3><p class="bg-success">Invalid issues <span class="badge">${getIssuesByCategoryName(categoryName).size()}</span></p></h3>
                        <#else>
		                    <h3><p class="bg-success">Released issues with type ${categoryName} <span class="badge">${getIssuesByCategoryName(categoryName).size()}</span></p></h3>
                        </#if>
	                    <ul>
	                        <#list getIssuesByCategoryName(categoryName) as issue>
	                            <li>
	                            <div>
	                                <#list issue.defectIds as defect>
	                                    <span class="label label-danger">${defect}</span>
	                                </#list>
	
	                                <img alt="" src="https://ensemble.atlassian.net/images/icons/priorities/${issue.issue.priority.name?lower_case}.svg" title="${issue.issue.priority.name}" height="16" width="16">
	                                </img>
	
	                                <a href="${issue.url}">${issue.issue.key}: ${issue.issue.summary} </a>
	                                <span class="label label-warning">${(issue.fixedInFlowWebVersion! "")}</span>
	                                <#if (issue.isStatusOk)>
	                                    <span class="label label-success">${(issue.status! "")}</span>
	                                <#else>
	                                    <span class="label label-danger">${(issue.status! "")}</span>
	                                </#if>
	                                <#list issue.pullRequestIds as prId>
	                                    <a href="${configuration.gitBrowsePrsUrl + prId}"><span class="label label-warning">PR:${prId}</span> </a>
	                                </#list>
	                                <span class="label label-warning">${(issue.fixVersions! "")}</span>
	                            </div>
	                                <#if (issue.releaseNotes)??>
	                                    <ul><li><b>Release Notes: </b><em>${issue.releaseNotes}</em></li></ul>
	                                </#if>
	                                <#if (issue.impact)??>
	                                    <ul><li><b>Impact: </b><em>${issue.impact}</em></li></ul>
	                                </#if>
	                                <#if (issue.detailsOfChange)??>
	                                    <ul><li><b>Details of change: </b><em>${issue.detailsOfChange}</em></li></ul>
	                                </#if>
	                            </li>
	                        </#list>
	                    </ul>
	                </#if>
                </div>
            </div>
        </#list>

        <div class="row">
            <div class="col-md-8">
                <h3><p class="bg-success">Commits with Defects <span class="badge">${commitsWithDefectIds.size()}</span></p></h3>
                <ul>
                    <#list commitsWithDefectIds as commit>
                        <li>
                            <#if commit.defectIds?has_content>
                                <#list commit.defectIds as defectId>
                                    <span class="label label-danger">${defectId}</span>
                                </#list>
                            </#if>
[id: ${commit.id}] ${commit.author} ${commit.message}
                        </li>
                    </#list>
                </ul>
            </div>
        </div>

        <div class="row">
            <div class="col-md-8">
                <h3><p class="bg-success">All defects </p></h3>
                <ol>
                    <#list uniqueDefects as defect>
                        <li>
                            <span class="label label-danger">${defect}</span>
                        </li>
                    </#list>
                </ol>
            </div>
        </div>
        <div class="row">
            <div class="col-md-8">
                <h3><p class="bg-success">Link to JIRA</p></h3>
                <a href="${jqlLink}">Link to JIRA</a>
                <p/>
            </div>
        </div>
        <div class="row">
            <div class="col-md-8">
                <h3><p class="bg-success">Known Issues <span class="badge">${knownIssues.size()}</span></p></h3>
                <#if (knownIssues.size() > 0)>
		            <a href="${knownIssuesJqlLink}">Link to JIRA</a>
                </#if>
                <#if (!getKnownIssuesErrorMessage().isEmpty())>
	                <p>Error: ${getKnownIssuesErrorMessage()}</p>
                </#if>
                <ul>
                    <#list getKnownIssues() as issue>
                        <li>
                            <#list issue.defectIds as defect>
                                <span class="label label-danger">${defect}</span>
                            </#list>
                            <img alt="" src="https://ensemble.atlassian.net/images/icons/priorities/${issue.issue.priority.name?lower_case}.svg" title="${issue.issue.priority.name}" height="16" width="16">
                            </img>
                            <div>
                                <a href="${issue.url}">${issue.issue.key}: ${issue.issue.summary} </a>
                                <span class="label label-warning">${(issue.fixedInFlowWebVersion! "")}</span>
                                <#if (issue.isStatusOk)>
                                    <span class="label label-success">${(issue.status! "")}</span>
                                <#else>
                                    <span class="label label-danger">${(issue.status! "")}</span>
                                </#if>
                                <#list issue.pullRequestIds as prId>
                                    <a href="${configuration.gitBrowsePrsUrl + prId}"><span class="label label-warning">PR:${prId}</span> </a>
                                </#list>
                                <span class="label label-warning">${(issue.fixVersions! "")}</span>
                            </div>
                            <#if (issue.releaseNotes)??>
                                <ul><li><b>Release Notes: </b><em>${issue.releaseNotes}</em></li></ul>
                            </#if>
                            <#if (issue.impact)??>
                                <ul><li><b>Impact: </b><em>${issue.impact}</em></li></ul>
                            </#if>
                            <#if (issue.detailsOfChange)??>
                                <ul><li><b>Details of change: </b><em>${issue.detailsOfChange}</em></li></ul>
                            </#if>
                        </li>
                    </#list>
                </ul>
            </div>
        </div>
</body>
</html>
</#escape>
                