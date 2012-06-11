<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short"/></a>
	</li>
	<li <c:if test='<%= request.getRequestURI().contains("spreadsheetimportForm") %>'>class="active"</c:if>>
		<a href="spreadsheetimport.form">
			New Import Template
		</a>
	</li>
	<li <c:if test='<%= request.getRequestURI().contains("spreadsheetimportTemplateList") %>'>class="active"</c:if>>
		<a href="spreadsheetimport.list">
			Import Templates
		</a>
	</li>
</ul>