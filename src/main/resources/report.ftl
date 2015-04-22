<html>
<head>
  <title>Release notes for version ${version}</title>
</head>
<body>
  <h1>Release notes for version ${version}</h1>

  <#list issues.entrySet() as entry>
	  <p>Released issues with type ${entry.key} (${entry.value.size()})</p>
	  <ul>
	    <#list entry.value as issue>
	      <li>[${issue.priority.name}] <a href="${jiraUrl}/browse/${issue.key}">${issue.key}: ${issue.summary}</a></li>
	    </#list>
	  </ul>
	  <br/>
	  
  </#list>

</body>
</html>