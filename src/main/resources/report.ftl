<html>
<head>
  <title>Release notes for version ${version}</title>
</head>
<body>
  <h1>Release notes for version ${version}</h1>

  <#list issues.entrySet() as entry>
	  <p>Released issues with type ${entry.key}:</p>
	  <ul>
	    <#list entry.value as issue>
	      <li><a href="${jiraUrl}/browse/${issue.key}">${issue.priority.name} ${issue.key}: ${issue.summary}</a></li>
	    </#list>
	  </ul>
	  <br/>
	  
  </#list>

</body>
</html>