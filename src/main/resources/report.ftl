<#escape x as x?html>
<html>
<head>
    <title>Release notes for version ${releaseVersion}</title>
</head>
<body>

<h1>Release notes for version ${releaseVersion}</h1>

<#list issueCategoryNames as categoryName>
    <p>Released issues with type ${categoryName} (${getIssuesByCategoryName(categoryName).size()})</p>
    <ul>
        <#list getIssuesByCategoryName(categoryName) as issue>
            <li>
                <b>${(issue.defectId) ! "none"}</b>

                [${issue.issue.priority.name}] <a href="${issue.url}">${issue.issue.key}: ${issue.issue.summary}</a>
            </li>
        </#list>
    </ul>
    <br/>

</#list>

<h2>Commits without JIRA issues (${commitsWithNoIssue.size()})</h2>
<ul>
    <#list commitsWithNoIssue as commit>
        <li>
            <#if commit.defectIds?has_content>
                <#list commit.defectIds as defectId>
                    <b>${defectId}</b>
                </#list>
                <#else>
                    <b>none</b>
            </#if>
            [id: ${commit.id}] ${commit.message}
        </li>
    </#list>
</ul>

</body>
</html>
</#escape>