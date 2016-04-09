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
<h1><p class="bg-primary">Release notes for version ${releaseVersion}</p></h1>
</div>
</div>


<#list issueCategoryNames as categoryName>
<div class="row">
    <div class="col-md-8">
        <h3><p class="bg-success">Released issues with type ${categoryName} <span class="label label-success">${getIssuesByCategoryName(categoryName).size()}</span></p></h3>
    <ul>
        <#list getIssuesByCategoryName(categoryName) as issue>

            <li>
            <#list issue.defectIds as defect>
                <span class="label label-info">${defect}</span>
            </#list>


                [${issue.issue.priority.name}] <a href="${issue.url}">${issue.issue.key}: ${issue.issue.summary} <span class="label label-warning">${(issue.fixedInFlowWebVersion! "")}</span></a>
            </li>
        </#list>
    </ul>
  </div>
</div>
</#list>

<div class="row">
    <div class="col-md-8">
<h3>Commits without JIRA issues <span class="label label-success">${commitsWithNoIssue.size()}</span></h3>
<ul>
    <#list commitsWithNoIssue as commit>
        <li>
            <#if commit.defectIds?has_content>
                <#list commit.defectIds as defectId>
                    <span class="label label-info">${defectId}</span>
                </#list>
            </#if>
            [id: ${commit.id}] ${commit.author} ${commit.message}
        </li>
    </#list>
</ul>
</div>
</div>

</body>
</html>
</#escape>
